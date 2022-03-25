package com.lcb.gmall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 *
 */
@EnableFeignClients(basePackages = "com.lcb.gmall.member.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GMallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GMallMemberApplication.class,args);
    }
}
