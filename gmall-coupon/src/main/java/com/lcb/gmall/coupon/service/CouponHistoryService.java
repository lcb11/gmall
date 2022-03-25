package com.lcb.gmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.coupon.entity.CouponHistoryEntity;

import java.util.Map;

/**
 * 优惠券领取历史记录
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:14:32
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

