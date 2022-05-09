package com.lcb.gmall.order.vo;

import com.lcb.gmall.order.entity.OrderEntity;
import lombok.Data;

/**
 *
 */
@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;
    private Integer code;//0成功
}
