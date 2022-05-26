package com.lcb.gmall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.lcb.common.to.mq.OrderTo;
import com.lcb.common.to.mq.StockDetailTo;
import com.lcb.common.to.mq.StockLockedTo;
import com.lcb.common.utils.R;
import com.lcb.gmall.ware.entity.WareOrderTaskDetailEntity;
import com.lcb.gmall.ware.entity.WareOrderTaskEntity;
import com.lcb.gmall.ware.service.WareSkuService;
import com.lcb.gmall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 *
 */

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {

        System.out.println("收到解锁库存的消息");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),true);
        }



    }

    public void handlerOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭准备解锁库存...");

        try {
            wareSkuService.unlockStock(orderTo);
        }catch (Exception e){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),true);
        }


    }
}
