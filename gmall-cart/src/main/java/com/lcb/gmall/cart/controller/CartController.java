package com.lcb.gmall.cart.controller;


import com.lcb.gmall.cart.interceptor.CartInterceptor;
import com.lcb.gmall.cart.service.CartService;
import com.lcb.gmall.cart.vo.Cart;
import com.lcb.gmall.cart.vo.CartItem;
import com.lcb.gmall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.jws.WebParam;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 *
 */
@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/currentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gmall.com//cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){

        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gmall.com//cart.html";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){

        cartService.checkItem(skuId,check);
        return "redirect:http://cart.gmall.com//cart.html";
    }

    /*
      * @Author lcb
      * @Description  获取临时购物车可以使用浏览器的cookie保存
      * @Date 2022/4/28
      * @Param [session]
      * @return java.lang.String
      **/
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
       /* UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println(userInfoTo);*/
        //获取购物车数据
        Cart cart=cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }


    //添加商品到购物车
    @GetMapping("/addCartItem")
    public String addCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                          RedirectAttributes ra) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId,num);
        ra.addAttribute("skuId",skuId);
        //http://cart.gmall.com/
        return "redirect:http://cart.gmall.com//addCartItemSuccess.html";
    }

    //跳到成功页
    @GetMapping("/addCartItemSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model){
        //重定向到成功页面，再次查询购物车数据即可
        CartItem item=cartService.getCartItem(skuId);
        model.addAttribute("item",item);
        return "success";
    }

}
