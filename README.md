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

|        技术        |           说明           |                      官网                       |
| :----------------: | :----------------------: | :---------------------------------------------: |
|     SpringBoot     |       容器+MVC框架       |     https://spring.io/projects/spring-boot      |
|    SpringCloud     |        微服务架构        |     https://spring.io/projects/spring-cloud     |
| SpringCloudAlibaba |        一系列组件        | https://spring.io/projects/spring-cloud-alibaba |
|    MyBatis-Plus    |         ORM框架          |             https://mp.baomidou.com             |
|  renren-generator  | 人人开源项目的代码生成器 |   https://gitee.com/renrenio/renren-generator   |
|   Elasticsearch    |         搜索引擎         |    https://github.com/elastic/elasticsearch     |
|      RabbitMQ      |         消息队列         |            https://www.rabbitmq.com             |
|   Springsession    |        分布式缓存        |    https://projects.spring.io/spring-session    |
|      Redisson      |         分布式锁         |      https://github.com/redisson/redisson       |
|       Docker       |       应用容器引擎       |             https://www.docker.com              |
|        OSS         |        对象云存储        |  https://github.com/aliyun/aliyun-oss-java-sdk  |


## 环境搭建

### 开发工具

|     工具      |        说明         |                      官网                       |
| :-----------: | :-----------------: | :---------------------------------------------: |
|     IDEA      |    开发Java程序     |     https://www.jetbrains.com/idea/download     |
| RedisDesktop  | redis客户端连接工具 |        https://redisdesktop.com/download        |
|  SwitchHosts  |    本地host管理     |       https://oldj.github.io/SwitchHosts        |
|    X-shell    |  Linux远程连接工具  | http://www.netsarang.com/download/software.html |
|    Navicat    |   数据库连接工具    |       http://www.formysql.com/xiazai.html       |
| PowerDesigner |   数据库设计工具    |             http://powerdesigner.de             |
|    Postman    |   API接口调试工具   |             https://www.postman.com             |
|    Jmeter     |    性能压测工具     |            https://jmeter.apache.org            |


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
1、在nginx.conf中添加负载均衡的配置   
upstream gmall{
	# 网关的地址
	server 192.168.114.1:88;
}    
2、在gmall.conf中添加如下配置
server {
	# 监听以下域名地址的80端口
    listen       80;
    server_name  gmall.com  *.gmall.com;

    #charset koi8-r;
    #access_log  /var/log/nginx/log/host.access.log  main;

    #配置静态资源分离
    location /static/ {
        root   /usr/share/nginx/html;
    }

    #支付异步回调的一个配置
    location /payed/ {
        proxy_set_header Host order.gmall.com;        #不让请求头丢失
        proxy_pass http://gmall;
    }

    location / {
        #root   /usr/share/nginx/html;
        #index  index.html index.htm;
        proxy_set_header Host $host;        #不让请求头丢失
        proxy_pass http://gmall;
    }
```
