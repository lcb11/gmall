package com.lcb.gmall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *
 */
@EnableDiscoveryClient
@MapperScan("com.lcb.gmall.product.dao")
@SpringBootApplication
public class GMallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GMallProductApplication.class,args);
    }
}
