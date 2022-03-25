package com.lcb.gmall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GMallOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(GMallOrderApplication.class,args);
    }
}
