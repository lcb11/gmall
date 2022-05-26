package com.lcb.gmall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *开启定时任务
 * @Scheduled 开启定时任务
 */
@Slf4j
@Component
//@EnableAsync//开启异步任务
//@EnableScheduling
public class HelloSchedule {

    /*
      * @Author lcb
      * @Description  使用异步+定时任务来完成定时任务不阻塞功能
      * @Date 2022/5/23
      * @Param []
      * @return void
      **/
    /*@Async//异步执行的方法
    @Scheduled(cron = "* * * * * ?")
    public void hello() throws InterruptedException {
        log.info("hello.....");
        Thread.sleep(3000);
    }*/
}
