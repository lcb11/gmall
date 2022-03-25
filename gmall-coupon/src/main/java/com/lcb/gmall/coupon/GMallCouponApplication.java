package com.lcb.gmall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GMallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GMallCouponApplication.class,args);
    }
}
