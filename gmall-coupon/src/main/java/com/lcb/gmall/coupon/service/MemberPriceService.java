package com.lcb.gmall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.coupon.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:14:32
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

