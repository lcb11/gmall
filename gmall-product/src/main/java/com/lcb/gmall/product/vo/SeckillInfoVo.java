package com.lcb.gmall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 *
 */
@Data
public class SeckillInfoVo {
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    //商品秒杀开始时间
    private Long startTime;

    //商品秒杀结束时间
    private Long endTime;
    //商品秒杀随机码
    private String randomCode;
}
