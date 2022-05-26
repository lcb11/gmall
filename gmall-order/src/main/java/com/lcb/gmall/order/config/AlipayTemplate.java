package com.lcb.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;

import com.lcb.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2088621958752560";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key ="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7hNfVPfAurvEnyxY362+6d+ZM1RxyRA6ea23YLFtnV4YxAVhAToLXpc/b1Twrv2sOFelRqNBKt6q7J1ajPh+Zs7Z6ccf3+iPJa5ezj0/3uh30EqNjrV0x35d3NlWHcyFEH3tXkjl1t/ee2waj9LwJQGHYqzozBOuYROOpZY7R/Z8ykeHe7sYeKmXyFx7S0GyHrcw8CI8UwzInNr7mefRhwyS/RhGKZOuH9LBxcTHTqpgf3SdsJaKe3+QlnJlG05b+DgTvGNxOiC1G/W56RTaN/jRUncoZ4E+NiUGCWtmvKA9KrAcU5vczkk4cTK5PpUNz7Zj9zCoT9X2iZ1q0VHTBAgMBAAECggEALHs0n5bTtJ83AVOBb75fgIIUlUEwQEK4nQeqxM6w1/DBGjrRbl+xVeo1RlZssYv0nyC/eelrj23GCCMqwApqgJFKxfzbjI0/k/X7xFJIi+EQXedV7r0fEEeTrGmtHDZJO9mj7n/3NV9wU7Rs/NN/D0rAax8i7RtLr5EX2AUJPfJcdN2jJ69VeGxG6Uz7yXc17hlte8QglqgCLlsfYnqgEg8gEfXbI0S4axxE64wI8LpMoGE+Z27/IzfCdw+omUE96S1Bk7rCsUN0m0vqdIwLqt+/qJ5XOTo6Pt5H3Rch+YFf4oY7w7OM4XODyYYcGVcalvig4FyeRIVdjujYHB0LCQKBgQD/j8n0KBvBN7xSEf3k4xcHXTjHpN9+wq0KutAE02G1vQD1+zCEqj0mWQDGZY7BLjFqVKcpxqAsU8HmmEVt1zGSm6Tp89H6SnV8XB2btZvI5/VsMouelmqwpmMLGqSfY9eTEcGen0NupfQz2M7gCXWLXba0KeBpK93N11g3IbUhewKBgQC71y2hSMgylZR1EnUHo1UnB596NoEGqG5Wvo0qa7io4Qq8PBlJaD48m1Q22k1nP1sezCKcxmZczG+sWuH+iAHN78/qH5kdCL9NkxhgP009trVy3A1dOAP47YenWRKO/cOq04qQWCQIyKE2AqUiwqBKWQS8soAt1dHlq2cGPsj38wKBgQCIA784cyBSdZeFFo5Kg5J+GN9fYiCRFouCamrMJrSaRT19rXLlKLXi8vu2m9aeejdSoDtXwJ7++JT6ZZCOJTn9DVl4KoxW6codpekcNkvzYYD9VOl6PhQKcIPGJSf4rOrPG2QxSBJbkXenIHz6QA6PXhEfUipdzzr9kt8geJAugQKBgHV++ZnjTublTcGVVAL9FeyeBhGKOlcR0EMOWnroes+YRQDNjvut6xA6EQlmr/gfVz4CbrwmFi2B63CiQK7YIFjS4vkN3tacfB1oH11E+2nY+dLo9qsSuNliqvHw83ziGxylygzUNWYRKVYBfY4qs90Npbrq5ObG0s6Y0o7Dgy4lAoGACETWFoYxcGBrhR7+fhUPTTzdjd1fhd3pakoKeTF8USzzCSjUCtZ8op+ndv2fCdtMo9o9sTziH6U8GZhjzgprc9T2c/FgcS7rmezNfmkYyvmxfVQJsbPq7YN0EQodrF3Wgq3F97aVstRzFZs1Mul7bkd/jBGAeVW76qFWgZii5i8=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArARUVx/VUP5D+nOwDeeAoTwoBmglVKRs6ZO8P7pK27Oiig5JZgyy8zywvBjCEgbB3BvvGVmgzXknn5BQALjTTnEAFdB0zWCL8SN9mHIlO6AdXio8oLsObItejSBdEw34I616r5zkWdBw4Q7+ziaV0iUMcnCC6MTkxPOAKSxLr01mX6vgnm6BA0G9iitbnX6IzChOEOSmJqpDERjD+8LXh81zLtHaGb0f00F6Id6Vag7mqGO/HvFHk3w/Si8PrwlLi+frqe1aog9hyu56GLD5yXRDZsX2o347hnXw/TYIh9D4XaKBOTfldRTeMd6QN9y3ejxVas5U3snuLfiUk9l7kwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url="http://2f5h954458.qicp.vip/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url="http://member.gmall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    private String timeout="30m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                +"\"timeout_express\":\""+timeout+"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
