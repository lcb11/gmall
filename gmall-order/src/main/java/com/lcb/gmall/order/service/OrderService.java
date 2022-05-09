package com.lcb.gmall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lcb.common.utils.PageUtils;
import com.lcb.gmall.order.entity.OrderEntity;
import com.lcb.gmall.order.vo.OrderConfirmVo;
import com.lcb.gmall.order.vo.OrderSubmitVo;
import com.lcb.gmall.order.vo.SubmitOrderResponseVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author lcb
 * @email 2990024235@qq.com
 * @date 2022-03-22 15:49:40
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    //订单确认页返回需要用的数据
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);
}

