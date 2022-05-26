package com.lcb.gmall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lcb.common.to.mq.SeckillOrderTo;
import com.lcb.common.utils.R;
import com.lcb.gmall.seckill.feign.CouponFeignService;
import com.lcb.gmall.seckill.feign.ProductFeignService;
import com.lcb.gmall.seckill.interceptor.LoginUserInterceptor;
import com.lcb.gmall.seckill.service.SeckillService;
import com.lcb.gmall.seckill.to.SeckillSkuRedisTo;
import com.lcb.gmall.seckill.vo.SeckillSessionsWithSkus;
import com.lcb.gmall.seckill.vo.SkuInfoVo;
import com.lcb.vo.MemberResVo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX ="seckill:skus";

    private final String SKU_STOCK_SEMAPHORE="seckill:stock:";//+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1、扫描最近3天参与秒杀的活动
        R session = couponFeignService.getLates3DaySession();
        if (session.getCode() == 0) {
            //上架商品
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1、缓存活动信息
            saveSessionInfos(sessionData);
            //2、缓存活动的关联商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    //返回当前时间可以参与的秒杀商品信息
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1、确定当前时间属于那个秒杀场次
        long time = new Date().getTime();

        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);

            if(time>=start&&time<=end){
                //2、获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if(list!=null){
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redis = JSON.parseObject((String) item, SeckillSkuRedisTo.class);
                        //redis.setRandomCode("");当前秒杀开始了就需要随机码
                        return redis;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }

        //2、获取这个秒杀场次需要的所有商品信息
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //1、找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        Set<String> keys = hashOps.keys();
        if(keys!=null&&keys.size()>0){
            String reg="\\d_"+skuId;
            for (String key : keys) {
                boolean matches = Pattern.matches(reg, key);
                if(matches){
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    //随机码
                    Long startTime = skuRedisTo.getStartTime();
                    Long endTime = skuRedisTo.getEndTime();
                    long now = new Date().getTime();
                    if(now>=startTime&&now<=endTime){
                    }else {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        //MemberResVo resVo = LoginUserInterceptor.threadLocal.get();
        //1、获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        String s = hashOps.get(killId);
        if(StringUtils.isEmpty(s)){
            return null;
        }else {
            SeckillSkuRedisTo redis = JSON.parseObject(s, SeckillSkuRedisTo.class);
            //2、校验合法性
            Long startTime = redis.getStartTime();
            Long endTime = redis.getEndTime();
            long ttl=endTime-startTime;
            //校验时间合法性
            if(new Date().getTime()>=startTime&&new Date().getTime()<=endTime){
                //校验随机码是否正确
                String randomCode = redis.getRandomCode();
                String skuId =redis.getPromotionSessionId()+"_"+redis.getSkuId();
                if(randomCode.equals(key)&&killId.equals(skuId)){
                    //验证购物数量是否合理
                   if(num<= redis.getSeckillLimit()){
                       //验证这个人是否购买过 幂等性；如果秒杀成功就去占位
                       //String redisKey=resVo.getMemberId()+"_"+skuId;
                       //自动过期
                       //Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MICROSECONDS);
                       //if(aBoolean){
                       if(true){
                           //占位成功说明没买过
                           RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                               boolean b = semaphore.tryAcquire(num);
                               if(b){
                                   //秒杀成功
                                   //快速下单，发送mq消息
                                   String timeId = IdWorker.getTimeId();
                                   SeckillOrderTo orderTo = new SeckillOrderTo();
                                   orderTo.setOrderSn(timeId);
                                   //orderTo.setMemberId(resVo.getMemberId());
                                   orderTo.setNum(num);
                                   orderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                   orderTo.setSkuId(redis.getSkuId());
                                   orderTo.setSeckillPrice(redis.getSeckillPrice());
                                   rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",orderTo);
                                   return timeId;
                               }else {
                                   return null;
                               }

                       }else {
                           //说明已经买过了
                           return null;
                       }

                   }
                }else {
                    return null;
                }
            }else {
                return null;
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            if(!hasKey){
                List<String> collect = session.getRelationSkus().stream().map(item ->item.getPromotionSessionId()+"_"+item.getSkuId().toString()).collect(Collectors.toList());
                //缓存活动信息
                redisTemplate.opsForList().leftPushAll(key,collect);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {

        sessions.stream().forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                //4、🎨设置随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                if(!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString())){
                    //缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //1、sku基本数据

                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if(skuInfo.getCode()==0){
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(info);
                    }
                    //2、sku秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);
                    //3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());


                    redisTo.setRandomCode(token);

                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId().toString()+"_"+seckillSkuVo.getSkuId().toString(),jsonString);
                    //如果当前场次商品库存信息已经上架就不需要上架了
                    //5、使用库存引入分布式信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    //将商品库存作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
