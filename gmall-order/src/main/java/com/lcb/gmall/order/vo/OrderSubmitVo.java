package com.lcb.gmall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 *封装订单提交数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId;//收获地址
    private Integer payType;//支付方式
    private String orderToken;//令牌
    private BigDecimal payPrice;//应付价格
    private String node;//订单备注信息

    //用户相关信息都在session中；直接在session中取出

}
