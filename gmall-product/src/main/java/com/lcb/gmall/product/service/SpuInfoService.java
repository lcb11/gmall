package com.lcb.gmall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.product.entity.SpuInfoDescEntity;
import com.lcb.gmall.product.entity.SpuInfoEntity;
import com.lcb.gmall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-21 15:40:21
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);

    void up(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

