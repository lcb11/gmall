package com.lcb.gmall.cart.service;

import com.lcb.gmall.cart.vo.Cart;
import com.lcb.gmall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public interface CartService {

    //将商品添加到购物车
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    //获取购物车某个购物项
    CartItem getCartItem(Long skuId);

    //获取整个购物车
    Cart getCart() throws ExecutionException, InterruptedException;

    //清空购物车
   void clearCart(String cartkey);

   //勾选购物项
    void checkItem(Long skuId, Integer check);

    //修改购物项数量
    void changeItemCount(Long skuId, Integer num);

    //删除购物项
    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();

}
