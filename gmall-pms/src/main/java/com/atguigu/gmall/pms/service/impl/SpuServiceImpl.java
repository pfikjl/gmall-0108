package com.atguigu.gmall.pms.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.service.SpuService;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

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

}