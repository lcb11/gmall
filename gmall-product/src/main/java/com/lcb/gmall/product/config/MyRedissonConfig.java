package com.lcb.gmall.product.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.api.RedissonClient;
import java.io.IOException;

/**
 *
 */
@Configuration
public class MyRedissonConfig {

    /*
      * @Author lcb
      * @Description  所有对Redisson的使用都是通过RedissonClient对象
      * @Date 2022/4/14
      * @Param []
      * @return org.redisson.api.RedissonClient
      **/
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        //创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.248.135:6379");
        //根据config创建出RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
