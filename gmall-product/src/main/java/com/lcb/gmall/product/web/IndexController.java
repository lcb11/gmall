package com.lcb.gmall.product.web;

import com.lcb.gmall.product.entity.CategoryEntity;
import com.lcb.gmall.product.service.CategoryService;
import com.lcb.gmall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        //TODO 查询一级分类
        List<CategoryEntity> categoryEntitys=categoryService.getLevel1Category();


        //视图解析器进行拼串处理
        model.addAttribute("categorys",categoryEntitys);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){

        Map<String, List<Catelog2Vo>> catalogJson=categoryService.getCatalogJson();
        return catalogJson;
    }


    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //获取一把锁
        RLock lock = redisson.getLock("my-lock");
        //加锁
        lock.lock();
        try {
            System.out.println("加锁成功");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //解锁
            System.out.println("释放锁");
            lock.unlock();
        }
        return "hello";
    }

    @GetMapping("/write")
    @ResponseBody
    public String writeLock(){

        return null;
    }
}
