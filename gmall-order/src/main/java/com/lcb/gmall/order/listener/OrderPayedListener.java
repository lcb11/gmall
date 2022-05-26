package com.lcb.gmall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.lcb.gmall.order.config.AlipayTemplate;
import com.lcb.gmall.order.service.OrderService;
import com.lcb.gmall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RestController
public class OrderPayedListener {

    @Autowired
    OrderService orderService;

    @Autowired
    AlipayTemplate alipayTemplate;

    @PostMapping("/payed/notify")
    public String handAlipayed(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException {
        //只要收到支付宝给我们的异步通知，告诉我们订单支付成功，返回success，支付包就不会再发通知
       /* Map<String, String[]> parameterMap = request.getParameterMap();
        for (String s : parameterMap.keySet()) {
            String value=request.getParameter(s);
            System.out.println("参数名："+s+"==>参数值:"+value);
        }
         System.out.println(parameterMap);*/

       //验证签名
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名

        if (signVerified) {
            System.out.println("签名验证成功...");
            //去修改订单状态
            String result = orderService.handlePayResult(vo);
            return result;
        } else {
            System.out.println("签名验证失败...");
            return "error";
        }
    }
}
