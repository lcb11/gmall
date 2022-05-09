package com.lcb.gmall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 *订单确认页需要用的数据
 */

public class OrderConfirmVo {

    //收货地址
    @Getter @Setter
    List<MemberAddressVo> address;

    //所有选中购物项
    @Getter @Setter
    List<OrderItemVo> items;

    //发票信息，发票记录

    //优惠卷信息
    @Getter @Setter
    Integer integration;

    //防重令牌
    @Getter @Setter
    String orderToken;
    //订单总额
    //BigDecimal total;

    public BigDecimal getTotal() {
        BigDecimal sum=new BigDecimal("0");
        if(items!=null){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum=sum.add(multiply);
            }
        }
        return sum;
    }

    //应付价格
 //   BigDecimal payPrice;

    public BigDecimal getPayPrice() {
       return getTotal();
    }
}


