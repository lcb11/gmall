# 谷粒商城

![](https://img.shields.io/badge/building-passing-green.svg)![GitHub](https://img.shields.io/badge/license-MIT-yellow.svg)![jdk](https://img.shields.io/static/v1?label=oraclejdk&message=8&color=blue)


## 项目介绍

谷粒商城项目是一套电商项目，包括前台商城系统以及后台管理系统，前台商城系统包括：用户登录、注册、商品搜索、商品详情、购物车、订单、秒杀活动等模块。后台管理系统包括：系统管理、商品系统、库存系统、订单系统、等模块。

## 组织结构

```
gmall
├── gmall-common -- 工具类及通用代码
├── renren-generator -- 人人开源项目的代码生成器
├── gmall-auth-server -- 认证中心（社交登录、OAuth2.0）
├── gmall-cart -- 购物车服务
├── gmall-coupon -- 优惠卷服务
├── gmall-gateway -- 统一配置网关
├── gmall-order -- 订单服务
├── gmall-product -- 商品服务
├── gmall-search -- 检索服务
├── gmall-seckill -- 秒杀服务
├── gmall-third-party -- 第三方服务（对象存储、短信）
├── gmall-ware -- 仓储服务
└── gmall-member -- 会员服务
```

## 技术选型

### 后端技术

|        技术         |                      官网                       |
| :----------------:  | :---------------------------------------------: |
|     SpringBoot     |    https://spring.io/projects/spring-boot      |
|    SpringCloud     |           https://spring.io/projects/spring-cloud     |
| SpringCloudAlibaba |                https://spring.io/projects/spring-cloud-alibaba |
|    MyBatis-Plus    |                             https://mp.baomidou.com             |
|  renren-generator  |    https://gitee.com/renrenio/renren-generator   |
|   Elasticsearch    |             https://github.com/elastic/elasticsearch     |
|      RabbitMQ      |                          https://www.rabbitmq.com             |
|   Springsession    |                 https://projects.spring.io/spring-session    |
|      Redisson      |                      https://github.com/redisson/redisson       |
|       Docker       |                     https://www.docker.com              |
|        OSS         |                 https://github.com/aliyun/aliyun-oss-java-sdk  |


## 环境搭建

### 开发工具

|     工具      |                         官网                       |
| :-----------: |   :---------------------------------------------: |
|     IDEA      |             https://www.jetbrains.com/idea/download     |
| RedisDesktop  |         https://redisdesktop.com/download        |
|  SwitchHosts  |             https://oldj.github.io/SwitchHosts        |
|    X-shell    |     http://www.netsarang.com/download/software.html |
|    Navicat    |          http://www.formysql.com/xiazai.html       |
| PowerDesigner |               http://powerdesigner.de             |
|    Postman    |                https://www.postman.com             |
|    Jmeter     |               https://jmeter.apache.org            |


### 开发环境

|     工具      | 版本号 |                             下载                             |
| :-----------: | :----: | :----------------------------------------------------------: |
|      JDK      |  1.8   | https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html |
|     Mysql     |  5.7   |                    https://www.mysql.com                     |
|     Redis     | Redis  |                  https://redis.io/download                   |
| Elasticsearch | 7.6.2  |               https://www.elastic.co/downloads               |
|    Kibana     | 7.6.2  |               https://www.elastic.co/cn/kibana               |
|   RabbitMQ    | 3.8.5  |            http://www.rabbitmq.com/download.html             |
|     Nginx     | 1.1.6  |              http://nginx.org/en/download.html               |


### 搭建步骤

> Windows环境部署

- 修改本机的host文件，映射域名端口至Nginx地址

```
192.168.129.114	gmall.com
192.168.129.114	search.gmall.com
192.168.129.114  item.gmall.com
192.168.129.114 auth.gmall.com
192.168.129.114 cart.gmall.com
192.168.129.114  order.gmall.com
192.168.129.114  member.gmall.com
192.168.129.114  seckill.gmall.com
```

- 修改Linux中Nginx的配置文件

```shell
server {
    listen       80;// 监听80端口
    server_name  gmall.com;// 监听的请求host，如果是gulimall.com则接收请求

    location / {
    	proxy_pass http://192.168.114.129:10000;// 将请求转发给商品服务
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
http {
    upstream gmall {// 上游服务器名
        server 192.168.114.129:88;// 负载均衡时配置多个网关地址
        server 192.168.114.129:89;
    }

    server {
        listen 80;
        server_name gmall.com;

        location / {
            proxy_pass http://gmall;// 代理给上游服务器
        }
    }
}
}
```
