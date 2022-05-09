package com.lcb.gmall.auth.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 */
@Configuration
public class GmallWebConfig implements WebMvcConfigurer{

    /*
      * @Author lcb
      * @Description  视图映射
      * @Date 2022/4/23
      * @Param [registry]
      * @GetMapping("/login.html")
      *  public String loginPage(){
      *     return "login";
      *      }
      **/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
