package com.lcb.gmall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Configuration
public class GmallFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor(){

            @Override
            public void apply(RequestTemplate template) {
                //拿到刚进来的请求数据
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = attributes.getRequest();
                //System.out.println("feign远程之前先进行RequestInterceptor");
                //同步请求头数据
                String cookie = request.getHeader("Cookie");
                //给新请求同步cookie
                template.header("Cookie",cookie);
            }
        };
    }
}
