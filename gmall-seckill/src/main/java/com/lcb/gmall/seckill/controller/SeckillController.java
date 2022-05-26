package com.lcb.gmall.seckill.controller;

import com.lcb.common.utils.R;
import com.lcb.gmall.seckill.service.SeckillService;
import com.lcb.gmall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 */
@RestController
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /*
      * @Author lcb
      * @Description 返回当前时间参与秒杀的商品信息
      * @Date 2022/5/25
      * @Param []
      * @return com.lcb.common.utils.R
      **/
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> vos=seckillService.getCurrentSeckillSkus();

        return R.ok().setData(vos);
    }

    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){


        SeckillSkuRedisTo to=seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }

    @GetMapping("/kill")
    public R seckill(@RequestParam("killId") String killId,
                     @RequestParam("key") String key,
                     @RequestParam("num") Integer num){

        String orderSn=seckillService.kill(killId,key,num);
        //1、判断是否登录

        return R.ok().setData(orderSn);
    }
}
