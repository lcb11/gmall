package com.lcb.gmall.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GmallThirdPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallThirdPartyApplication.class, args);
    }

}
