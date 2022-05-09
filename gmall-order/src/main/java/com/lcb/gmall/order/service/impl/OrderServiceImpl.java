package com.lcb.gmall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lcb.common.exception.NoStockException;
import com.lcb.common.utils.R;
import com.lcb.gmall.order.constant.OrderConstant;
import com.lcb.gmall.order.dao.OrderItemDao;
import com.lcb.gmall.order.entity.OrderItemEntity;
import com.lcb.gmall.order.enume.OrderStatusEnum;
import com.lcb.gmall.order.feign.CartFeignService;
import com.lcb.gmall.order.feign.MemberFeignService;
import com.lcb.gmall.order.feign.ProductFeignService;
import com.lcb.gmall.order.feign.WmsFeignService;
import com.lcb.gmall.order.interceptor.LoginUserInterceptor;
import com.lcb.gmall.order.service.OrderItemService;
import com.lcb.gmall.order.to.OrderCreateTo;
import com.lcb.gmall.order.vo.*;
import com.lcb.vo.MemberResVo;
import io.seata.spring.annotation.GlobalTransactional;
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
    OrderDao orderDao;
    @Autowired
    OrderItemDao orderItemDao;

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

    @GlobalTransactional
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
        //订单总额
        for (OrderItemEntity entity : itemEntities) {
            BigDecimal realAmount = entity.getRealAmount();
            total=total.add(realAmount);
        }
        //订单价格相关
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(new BigDecimal("10.0")));
        orderEntity.setPromotionAmount(new BigDecimal("0.0"));
        orderEntity.setIntegration(0);
        orderEntity.setCouponAmount(new BigDecimal("0.0"));
        orderEntity.setDeleteStatus(0);//未删除

    }

    private OrderEntity buildOrder(String orderSn) {
        OrderEntity entity = new OrderEntity();
        entity.setMemberId(123456L);
        entity.setOrderSn(orderSn);
        //2、获取收货地址信息（调用远程服务gmall-ware计算运费，这里直接设置
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        //设置收货人信息和运费信息
        entity.setFreightAmount(new BigDecimal("998"));
        entity.setReceiverCity("TUTE");
        entity.setReceiverDetailAddress("TUTE-9A-119-3");
        entity.setReceiverName("lcb");
        entity.setReceiverPhone("15907969753");
        entity.setReceiverProvince("天津河西");
        entity.setReceiverRegion("天津河西TUTE");

        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        return entity;
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