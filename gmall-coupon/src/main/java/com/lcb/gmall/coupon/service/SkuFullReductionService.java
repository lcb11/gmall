package com.lcb.gmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.to.SkuReductionTo;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:14:32
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo reductionTo);
}

