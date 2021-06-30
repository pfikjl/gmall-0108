package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;

import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
//@Transactional(rollbackFor = FileNotFoundException.class, noRollbackFor = ArithmeticException.class) //开启事务
//@Transactional(rollbackFor = Exception.class) //开启事务 设置都回滚
//@Transactional
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    /*@Autowired
    private SpuDescMapper spuDescMapper;*/
    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private SpuDescService spuDescService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(PageParamVo paramVo, Long categoryId) {
//select * from pms_spu where category_id=225 and (id=7 or `name` like '%7%');
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        //如果categoryId不为0 需要查本类 有分类id具体查询   如果为0就按后面的条件模糊查询
        if(categoryId != 0){
            wrapper.eq("category_id",categoryId);
        }
        //关键字
        String key = paramVo.getKey();  //考虑输入的不为空
        //isemoty没有考虑空格  isbank判断的含有否为空格
        if(StringUtils.isNotBlank(key)){
            //wrapper直接用.eq 默认为sql语句中的and 没有实现带括号的操作
            wrapper.and(t -> t.eq("id",key).or().like("name",key)); //.and 可以实现（）拼接操作
        }

        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) throws FileNotFoundException {

        //1、保存spu相关的3表
            //1.1  保存 pms_spu
        Long spuId = saveSpuInfo(spu);
        //1.2保存spu的描述信息 spu_info_desc
        this.spuDescService.saveSpuDesc(spu, spuId);//用本service方法调用不是动态代理对象 不会启动事务所以不生效 所用调用spuDescService
        //int i= 1/0;  // 非受检时异常 默认都回滚
        //new FileInputStream("xxxX");//受检时异常 默认都不回滚
        //1.3保存pms_spu_att_value   把SpuAttValueVo转换成SpuAttrValueEntity类型
        saveBaseAttrs(spu, spuId);
        //2、sku相关的3张表
        saveSkuInfo(spu, spuId);
        //int i= 1/0;

    }

    private void saveSkuInfo(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return ;
        }
        //遍历sku 保存到pms_sku
        skus.forEach(skuVo -> {
            //2.1pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
                //获取页面图片列表
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                // 取第一张图片为默认图片
                //判断前台是否传来的有默认图片 如果没有设置第一张为默认图片 如果有就设置为默认
                skuVo.setDefaultImage(StringUtils.isBlank(skuVo.getDefaultImage())? images.get(0):skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();

            //2.2pms_sku_images 图片具体信息
            if (!CollectionUtils.isEmpty(images)){

                this.skuImagesService.saveBatch(images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVo.getDefaultImage(),image)?1:0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            //2.3pms_sku_att_value  .
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity -> {
                    skuAttrValueEntity.setSkuId(skuId);
                });
            }
            this.skuAttrValueService.saveBatch(saleAttrs);

            //3.保存营销相关的三张表
            //sms_sku_bounds积分优惠
            //sms_sku_full_reduction满减
            //sms_sku_ladder打折
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.smsClient.saleSales(skuSaleVo);
        });
    }

    private void saveBaseAttrs(SpuVo spu, Long spuId) {
        List<SpuAttValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream()
                    .filter(spuAttValueVo -> spuAttValueVo.getAttrValue() != null)
                    .map(spuAttValueVo -> {
                        SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                        BeanUtils.copyProperties(spuAttValueVo, spuAttrValueEntity);//后面的是目标参数 用copyProperties方法不用一一设置属性
                        spuAttrValueEntity.setSpuId(spuId);
                        return spuAttrValueEntity;
                    }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }

    //spu详情信息放到SpuDescservice中了

    private Long saveSpuInfo(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

    public static void main(String[] args) {
        List<User> users = Arrays.asList(
            new User("xinhui",20,true),
            new User("laocao",21,true),
            new User("ningge",22,false),
            new User("feige",23,true),
            new User("haoge",24,false),
            new User("peifeng",25,true)
        );
        //map：把一个集合转换成另一个集合 eg： user-Person
/*        users.stream().map(User::getAge).collect(Collectors.toList()).forEach(System.out::println);
        users.stream().map(user -> {
            Person person = new Person();
            person.setAge(user.getAge());
            person.setName(user.getUsername());
            return person;
        }).collect(Collectors.toList()).forEach(System.out::println);*/

        //用filter 过滤出需要的元素 ，组装成新的集合
//        users.stream().filter(user -> user.getAge() > 22).collect(Collectors.toList()).forEach(System.out::println);
        users.stream().filter(user -> user.getSex() == false).collect(Collectors.toList()).forEach(System.out::println);
        users.stream().filter(User::getSex).collect(Collectors.toList()).forEach(System.out::println);

        //用reduce 求和
        List<Integer> arrs = Arrays.asList(21,22,23,34);
        System.out.println(arrs.stream().reduce((a,b) -> a+b).get());
        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());


    }

}
@Data
@AllArgsConstructor
@NoArgsConstructor
class User{
    private String username;
    private Integer age;
    private Boolean sex;
}

@Data
@ToString
class Person{
    private String name;
    private Integer age;
}