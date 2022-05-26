package com.lcb.gmall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lcb.common.exception.NoStockException;
import com.lcb.common.to.mq.OrderTo;
import com.lcb.common.to.mq.SeckillOrderTo;
import com.lcb.common.utils.R;
import com.lcb.gmall.order.dao.OrderItemDao;
import com.lcb.gmall.order.entity.OrderItemEntity;
import com.lcb.gmall.order.entity.PaymentInfoEntity;
import com.lcb.gmall.order.enume.OrderStatusEnum;
import com.lcb.gmall.order.feign.CartFeignService;
import com.lcb.gmall.order.feign.MemberFeignService;
import com.lcb.gmall.order.feign.ProductFeignService;
import com.lcb.gmall.order.feign.WmsFeignService;
import com.lcb.gmall.order.interceptor.LoginUserInterceptor;
import com.lcb.gmall.order.service.OrderItemService;
import com.lcb.gmall.order.service.PaymentInfoService;
import com.lcb.gmall.order.to.OrderCreateTo;
import com.lcb.gmall.order.vo.*;
import com.lcb.vo.MemberResVo;
import com.lcb.vo.PayVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcb.common.utils.PageUtils;
import com.lcb.common.utils.Query;

import com.lcb.gmall.order.dao.OrderDao;
import com.lcb.gmall.order.entity.OrderEntity;
import com.lcb.gmall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.lcb.gmall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal=new ThreadLocal<>();
    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderDao orderDao;


    @Autowired
    OrderItemDao orderItemDao;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
}

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResVo memberResVo = LoginUserInterceptor.threadLocal.get();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //远程查询所有的订单列表
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResVo.getMemberId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //远程查询购物车选中的所有购物项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor);

        //查询用户积分
        Integer integration = memberResVo.getIntegration();
        confirmVo.setIntegration(integration);

        //其他数据自动计算

        //防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX+memberResVo.getMemberId(),token,30, TimeUnit.SECONDS);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture,cartFuture).get();
        return confirmVo;
    }

    //@GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();

        //创建订单、验令牌、验价格、锁库存
        MemberResVo memberResVo = LoginUserInterceptor.threadLocal.get();

        response.setCode(0);
        //1、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();

        //通过lure脚本原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberResVo.getMemberId()),
                orderToken);

        //String redisToken = redisTemplate.opsForValue().get(USER_ORDER_TOKEN_PREFIX + memberResVo.getMemberId());*/
        if(result!=0L){
            //不通过
            response.setCode(1);
            return response;
        }else {
            //令牌通过[令牌对比删除保证原子性]
            //创建订单，订单项等信息
            OrderCreateTo order = createOrder();
            //验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //金额对比

                //保存订单
                saveOrder(order);
                //库存锁定  只要有异常，回滚订单数据 订单号
                //订单号，所有订单项（skuId skuName num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //远程锁库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode()==0){
                    //锁成功
                    response.setOrder(order.getOrder());
                    //TODO 订单创建成功，发送消息给mq
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order);
                    return response;
                }else {
                    //锁定失败
                    //throw new NoStockException();
                    /*response.setCode(3);
                    return response;*/
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }

            }else {
                response.setCode(2);
                return response;
            }

        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        //查询当前这个订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        //订单状态
        if(orderEntity.getStatus()==OrderStatusEnum.CREATE_NEW.getCode()){
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            //给mq发一个消息
            try {
                //TODO 保证消息一定发送出去，每一个消息做好日志记录(给数据库保存每个消息的详细信息，定期扫描数据库）
                rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
            }catch (Exception e){
                //TODO 将没发送成功的消息重新发送出去

            }

        }
    }

    /*
      * @Author lcb
      * @Description  获取订单支付信息
      * @Date 2022/5/21
      * @Param [orderSn]
      * @return com.lcb.vo.PayVo
      **/
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal decimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(decimal.toString());
        payVo.setOut_trade_no(order.getOrderSn());
        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberResVo memberResVo = LoginUserInterceptor.threadLocal.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberResVo.getMemberId()).orderByDesc("id")
        );

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);
        return new PageUtils(page);
    }

    /*
      * @Author lcb
      * @Description 处理支付宝的字符结果
      * @Date 2022/5/21
      * @Param [vo]
      * @return java.lang.String
      **/
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);
        //修改订单状态信息
        if(vo.getTrade_status().equals("TRADE_SUCCESS")||vo.getTrade_status().equals("TRADE_FINISHED")){
            //支付成功
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }
        return "success";

    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal("" + seckillOrderTo.getNum()));
        orderEntity.setPayAmount(multiply);

        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(seckillOrderTo.getNum());
        //TODO 获取当前sku详细信息
        //productFeignService.getSpuInfoBySkuId();
        orderItemService.save(orderItemEntity);
    }


    /*
      * @Author lcb
      * @Description 保存订单数据
      * @Date 2022/5/8
      * @Param [order]
      * @return void
      **/
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();

        //1、生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        orderCreateTo.setOrder(orderEntity);
        //获取订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        orderCreateTo.setOrderItems(itemEntities);
        //验价(计算价格相关
        computePrice(orderEntity,itemEntities);


        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        //优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;
        //订单总额
        for (OrderItemEntity entity : itemEntities) {
            //优惠价格信息
            coupon = coupon.add(entity.getCouponAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            intergration = intergration.add(entity.getIntegrationAmount());

            //总价
            total = total.add(entity.getRealAmount());

            //积分信息和成长值信息
            integrationTotal += entity.getGiftIntegration();
            growthTotal += entity.getGiftGrowth();

        }
        //1、订单价格相关的
        orderEntity.setTotalAmount(total);
        //设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        //设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        //设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);


    }

    private OrderEntity buildOrder(String orderSn) {

        //获取当前用户登录信息
        MemberResVo memberResponseVo = LoginUserInterceptor.threadLocal.get();

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberResponseVo.getMemberId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();

        /*//远程获取收货地址和运费信息
        R fareAddressVo = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fareAddressVo.getData("data", new TypeReference<FareVo>() {});*/

        /*//获取到运费信息
        BigDecimal fare = fareResp.getFare();
        orderEntity.setFreightAmount(fare);*/

        /*//获取到收货地址信息
        MemberAddressVo address = fareResp.getAddress();*/
       /* //设置收货人信息
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());*/

        //设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }

    /*
      * @Author lcb
      * @Description 构建所有订单项数据
      * @Date 2022/5/8
      * @Param [cartItem]
      * @return com.lcb.gmall.order.entity.OrderItemEntity
      **/
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null&&currentUserCartItems.size()>0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);

                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /*
      * @Author lcb
      * @Description 构建指定的订单项类容
      * @Date 2022/5/8
      * @Param [cartItem]
      * @return com.lcb.gmall.order.entity.OrderItemEntity
      **/
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        //订单信息
        OrderItemEntity itemEntity = new OrderItemEntity();
        //spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());
        //sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().intValue());
        //价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际金额
        itemEntity.setRealAmount(itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString())));
        return itemEntity;
    }

}