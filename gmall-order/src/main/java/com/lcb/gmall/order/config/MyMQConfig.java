package com.lcb.gmall.order.config;

import com.lcb.gmall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
@Configuration
public class MyMQConfig {


    @Bean //加到容器中
    public Queue orderDelayQueue(){
        Map<String,Object> arguments=new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);


        return  queue;
    }

    @Bean
    public Queue  orderReleaseQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }
    //秒杀队列，消峰用的
    @Bean
    public Queue  orderSeckillOrderQueue(){
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Exchange orderEventExchange(){
       return  new TopicExchange("order-event-exchange",true,false);
    }

    @Bean
    public Binding orderCreateOrderBingding(){
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",null);
    }

    @Bean
    public Binding orderReleaseOrderBingding(){
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",null);
    }

    /**
      * @Author lcb
      * @Description 订单释放和库存释放进行绑定
      * @Date 2022/5/18
      * @Param []
      * @return org.springframework.amqp.core.Binding
      **/
    @Bean
    public Binding orderReleaseOtherBingding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",null);
    }


    @Bean
    public Binding orderSeckillOrderQueueBingding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",null);
    }



}
