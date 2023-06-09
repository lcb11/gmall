package com.lcb.gmall.order.web;

import com.lcb.gmall.order.service.OrderService;
import com.lcb.gmall.order.vo.OrderConfirmVo;
import com.lcb.gmall.order.vo.OrderSubmitVo;
import com.lcb.gmall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 *
 */
@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        System.out.println(confirmVo);

        model.addAttribute("orderConfirmData", confirmVo);
        //展示订单确认的数据
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {

        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);

        //返回支付页
        //
        if (responseVo.getCode() == 0) {
            //成功
            model.addAttribute("submitOrderResp", responseVo);
            return "pay";
        } else {
            String msg = "下单失败;";
            switch (responseVo.getCode()) {
                case 1:
                    msg += "订单信息过期，请重新提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化";
                    break;
                case 3:
                    msg += "商品库存不足";
                    break;
            }
            redirectAttributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.gmall.com/toTrade";
        }
    }
}
