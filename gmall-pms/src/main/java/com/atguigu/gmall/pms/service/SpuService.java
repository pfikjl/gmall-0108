package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * spu信息
 *
 * @author wpf
 * @email 764545780@qq.com
 * @date 2021-06-22 19:41:22
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    PageResultVo querySpuByCidAndPage(PageParamVo paramVo, Long categoryId);

    void bigSave(SpuVo spu) throws FileNotFoundException;

    //void saveSpuDesc(SpuVo spu, Long spuId);//测试事务
}

