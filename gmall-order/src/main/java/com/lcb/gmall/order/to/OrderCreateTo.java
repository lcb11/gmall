package com.lcb.gmall.order.to;

import com.lcb.gmall.order.entity.OrderEntity;
import com.lcb.gmall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 */
@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;//价格

    private BigDecimal fare;//运费
}
