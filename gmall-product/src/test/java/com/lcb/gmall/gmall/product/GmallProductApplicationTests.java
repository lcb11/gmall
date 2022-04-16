package com.lcb.gmall.gmall.product;

import com.lcb.gmall.product.service.BrandService;
import com.lcb.gmall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@SpringBootTest
public class GmallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redissonClient;



    @Test
    public void redisson(){
        System.out.println(redissonClient);
    }

    @Test
    public void teststringRedisTemplate(){

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        ops.set("hello","world"+ UUID.randomUUID().toString());

        //查询
        String hello = ops.get("hello");
        System.out.println("之前保存的数据是："+hello);
    }
    /*@Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整录制："+catelogPath);
    }*/

   /* @Test
    public void contextLoads() {

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("huawei");
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功.....");
        brandService.updateById(brandEntity);
    }*/




   /* @Test
    public void Demo(){

            // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
            String endpoint = "oss-cn-beijing.aliyuncs.com";
            // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
            String accessKeyId = "LTAI5tCcpVTknLNWcbqzFbw2";
            String accessKeySecret = "yourAccessKeySecret";
            // 填写Bucket名称，例如examplebucket。
            String bucketName = "1A48lkaB8na7o9Mhx8M7RunHWjML8J";
            // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
            String objectName = "D:\\BaiduNetdiskDownload\\谷粒商城\\Guli Mall(包含代码、课件、sql)\\Guli Mall\\课件和文档(老版)\\基础篇\\资料\\pics\\28f296629cca865e.jpg";
            // 填写本地文件的完整路径，例如D:\\localpath\\examplefile.txt。
            // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
            String filePath= "D:\\localpath\\examplefile.txt";

            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            try {
                InputStream inputStream = new FileInputStream(filePath);
                // 创建PutObject请求。
                ossClient.putObject(bucketName, objectName, inputStream);
            } catch (OSSException oe) {
                System.out.println("Caught an OSSException, which means your request made it to OSS, "
                        + "but was rejected with an error response for some reason.");
                System.out.println("Error Message:" + oe.getErrorMessage());
                System.out.println("Error Code:" + oe.getErrorCode());
                System.out.println("Request ID:" + oe.getRequestId());
                System.out.println("Host ID:" + oe.getHostId());
            } catch (ClientException ce) {
                System.out.println("Caught an ClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with OSS, "
                        + "such as not being able to access the network.");
                System.out.println("Error Message:" + ce.getMessage());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        }*/

}
