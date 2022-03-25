package com.lcb.gmall.ware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GMallWareApplication {
    public static void main(String[] args) {
        SpringApplication.run(GMallWareApplication.class,args);
    }
}
