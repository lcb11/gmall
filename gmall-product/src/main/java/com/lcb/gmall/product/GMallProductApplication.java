package com.lcb.gmall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 *
 */
@EnableCaching
@EnableFeignClients(basePackages = "com.lcb.gmall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.lcb.gmall.product.dao")
@SpringBootApplication //(exclude = GlobalTransactionAutoConfiguration.class)
public class GMallProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(GMallProductApplication.class,args);
    }
}
