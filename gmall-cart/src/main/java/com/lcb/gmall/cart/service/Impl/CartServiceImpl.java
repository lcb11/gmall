package com.lcb.gmall.cart.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lcb.common.utils.R;
import com.lcb.gmall.cart.feign.ProductFeignService;
import com.lcb.gmall.cart.interceptor.CartInterceptor;
import com.lcb.gmall.cart.service.CartService;
import com.lcb.gmall.cart.vo.Cart;
import com.lcb.gmall.cart.vo.CartItem;
import com.lcb.gmall.cart.vo.SkuInfoVo;
import com.lcb.gmall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 *
 */

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;

    private final String CART_PREFIX="gmall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();



         String res = (String) cartOps.get(skuId.toString());

         if(StringUtils.isEmpty(res)){
             //2、将商品添加到购物车(新商品)
             CartItem cartItem = new CartItem();
             //购物车无此商品
             CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                 //1、远程查询当前要添加商品的信息
                 R skuInfo = productFeignService.getSkuInfo(skuId);
                 SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                 });

                 cartItem.setCheck(true);
                 cartItem.setCount(num);
                 cartItem.setImage(data.getSkuDefaultImg());
                 cartItem.setTitle(data.getSkuTitle());
                 cartItem.setPrice(data.getPrice());
                 cartItem.setSkuId(skuId);
             },executor);

             CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                 //3、远程查询sku的组合信息
                 List<String> values = productFeignService.getSkuSaleAttrValues(skuId);
                 cartItem.setSkuAttr(values);
             }, executor);

             CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();
             String s = JSON.toJSONString(cartItem);
             cartOps.put(skuId.toString(),s);
             return cartItem;
         }else {
             //购物车有这个商品，修改数量
             CartItem cartItem = JSON.parseObject(res, CartItem.class);
             cartItem.setCount(cartItem.getCount()+num);
             cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
             return cartItem;
         }






    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String s = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(s, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        //1、得到用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId()!=null){
            //1、登录状态
            String cartKey =CART_PREFIX+ userInfoTo.getUserId();
            //1、如果临时购物车还没合并
            String tempCartKey=CART_PREFIX + userInfoTo.getUserkey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if(tempCartItems!=null){
                //临时购物车有数据
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(),item.getCount());
                }
                //清除临时 购物车
                clearCart(tempCartKey);
            }
            //获取登录后的购物车(包含临时购物车）
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }else {
            //没登录
            String cartkey =CART_PREFIX+ userInfoTo.getUserkey();
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartkey);
            List<Object> values = hashOps.values();
            if(values!=null&&values.size()>0){
                List<CartItem> collect = values.stream().map(obj -> {
                    String str= (String) obj;
                    CartItem cartItem = JSON.parseObject(str, CartItem.class);
                    return cartItem;
                }).collect(Collectors.toList());
                cart.setItems(collect);
            }

        }
        return cart;
    }

    /*
      * @Author lcb
      * @Description 获取到要操作的购物车
      * @Date 2022/4/30
      * @Param []
      * @return org.springframework.data.redis.core.BoundHashOperations<java.lang.String,java.lang.Object,java.lang.Object>
      **/
    private BoundHashOperations<String, Object, Object> getCartOps() {
        //1、得到用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //2、
        String cartkey="";
        if(userInfoTo.getUserId()!=null){
            cartkey=CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartkey=CART_PREFIX+userInfoTo.getUserkey();
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartkey);
        return operations;
    }

    private List<CartItem> getCartItems(String cartKey){
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if(values!=null&&values.size()>0){
            List<CartItem> collect = values.stream().map(obj -> {
                String str= (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }

        return null;
    }

    @Override
    public void clearCart(String cartkey){
        redisTemplate.delete(cartkey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo.getUserId()==null){
            return null;
        }else {
            String cartkey=CART_PREFIX+userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartkey);
            //获取所有被选中的购物项
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item->{
                        R price = productFeignService.getPrice(item.getSkuId());
                        //TODO 更新为最新价格
                        String data = (String) price.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }
    }
}
