package com.lcb.gmall.thirdparty;

import com.aliyun.oss.OSS;
import com.lcb.gmall.thirdparty.component.SmsComponent;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
  * @Author lcb
  * @Description
  * @Date 2022/4/23
  * @Param
  * @return
  **/
@SpringBootTest
public class GmallThirdPartyApplicationTests {

    @Autowired
    SmsComponent smsComponent;

    @Autowired
    OSS ossClient;

    @Test
    public void smsTest(){

        smsComponent.sendSmsCode("15907969753","77777");

    }
}
