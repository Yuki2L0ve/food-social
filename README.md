# 美食社交APP后台开发
## 项目介绍

该项目以美食社交APP后台API接口设计为例。涉及APP中用户、好友、订单为基础的相关业务，分为用户、好友、排行榜、优惠券/秒杀、订单、附近的人、Feed等微服务。完成用户登录、交友、发朋友圈以及购买优惠券、下单整个业务流程，并实现积分排行榜以及附近的人等相关功能。

- 用户：实现用户注册、单点登录等功能
- 好友：实现关注、取关、互关等功能
- 排行榜：实现积分、热度排行版等功能
- 缓存：实现缓存餐厅数据等功能
- 秒杀：实现商品销售、商品倒计时秒杀等功能
- 订单：实现利用分布式锁解决多用户下单功能
- 附近的人：实现根据地理位置搜索附近的人等功能
- Feed流：实现添加、点赞、评论、列表等功能



美食社交APP的需求分析图：

![](G:\others\workspace\github\food-social\README.assets\1708400516399-93543933-c9e4-44d3-a6ae-6037476c78ad.png)



项目功能描述图：

![](G:\others\workspace\github\food-social\README.assets\1708400451898-1dad80cb-04c1-4b89-bd92-7b5e7adebb62.png)



## 项目功能实现

在本项目中，重点开发完成了如下表中的功能：

| 业务场景     | 数据类型        | 操作指令                           | 作用                                 |
| ------------ | --------------- | ---------------------------------- | ------------------------------------ |
| 单点登录     | String          | SET、GET                           | 存储 Token 与登录食客信息            |
| 抢购代金券   | Hash、Lua       | HGET、HSET、HINCRBY                | 防止超卖、限购(分布式锁)             |
| 好友功能     | Set             | SADD、SMEMBERS、SINTER             | 存储关注集合、粉丝集合、共同关注列表 |
| Feed功能     | Sorted Set      | ZADD、ZREVRANGE                    | 关注的好友的 Feed 流集合             |
| 签到功能     | Bitmap、String  | SETBIT、GETBIT、BITCOUNT、BITFIELD | 位图存储食客签到信息                 |
| 积分排行榜   | Sorted Set      | ZINCRBY、ZREVRANK、ZREVRANGE       | 存储食客总积分集合方便排序           |
| 附近的人     | Geo、Sorted Set | GEOADD、GEOREDIUS                  | 存储与查询食客地理位置信息           |
| 餐厅缓存     | Hash            | HSET、HGETALL、HINCRBY             | 存储餐厅热点数据                     |
| 最新餐厅评论 | List            | LPUSH、LRANGE                      | 存储最新餐厅评论                     |



### 单点登录

这里使用 Spring Security 和 OAuth2.0 实现了授权认证中心及单点登录的功能。这个功能中 Redis 主要用于存储 Token 令牌信息，使用了 String 数据类型。



### 抢购优惠券

这个功能中实现了抢购秒杀完整的一套业务，解决了超卖、限制一人一单的问题。

这个功能中 Redis 主要用于实现分布式锁、Lua脚本，使用了 Hash 数据类型，利用了原生的方式和 Redisson 的方式。



### 好友功能

这个功能中实现了关注、取关、获取共同关注列表功能。

这个功能中 Redis 主要用于存储关注列表和粉丝列表中的相关用户信息，使用了 Set 数据类型。



### Feed功能

这个功能中实现了添加 Feed、删除 Feed、关注取关时变更 Feed、查询 Feed 功能。

这个功能中 Redis 主要用于存储每个用户关注好友添加的 Feed 流集合，使用了 Zset 数据类型。



### 签到功能

这个功能中实现了签到、补签、获取连续签到次数、获取签到总次数、获取签到详情功能。

这个功能中 Redis 主要用于存储签到信息，使用了 Bitmap 数据类型。



### 积分功能

这个功能中实现了添加积分、获取积分排行榜功能。

这个功能中 Redis 主要用于存储积分信息，使用了 Sorted Set 数据类型。



### 附近的人

这个功能中实现了上传用户坐标、获取附近的人功能。

这个功能中 Redis 主要用于存储地理位置信息，使用了 GEO 数据类型。



### 餐厅缓存

这个功能中实现了餐厅热点数据缓存、查询餐厅缓存功能。

这个功能中 Redis 主要用于存储餐厅信息，使用了 Hash 数据类型。



### 最新餐厅评论

这个功能中实现了添加餐厅评论、获取餐厅评论功能。

这个功能中 Redis 主要用于存储餐厅评论信息，使用了 List 数据类型。



## 开发文档

文档是对项目开发过程中遇到的一些问题的详细记录，便于快速了解该项目，并且后期可以复盘巩固这个项目。

1. [项目需求分析](https://github.com/Yuki2L0ve/food-social/wiki/1-%E9%A1%B9%E7%9B%AE%E9%9C%80%E6%B1%82%E5%88%86%E6%9E%90)
2. [数据库设计](https://github.com/Yuki2L0ve/food-social/wiki/2-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%AE%BE%E8%AE%A1)
3. [项目架构](https://github.com/Yuki2L0ve/food-social/wiki/3-%E9%A1%B9%E7%9B%AE%E6%9E%B6%E6%9E%84)
4. [微服务基础环境搭建](https://github.com/Yuki2L0ve/food-social/wiki/4-%E5%BE%AE%E6%9C%8D%E5%8A%A1%E5%9F%BA%E7%A1%80%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA)
5. [抽取公共模块](https://github.com/Yuki2L0ve/food-social/wiki/5-%E6%8A%BD%E5%8F%96%E5%85%AC%E5%85%B1%E6%A8%A1%E5%9D%97)
6. [认证授权中心](https://github.com/Yuki2L0ve/food-social/wiki/6-%E8%AE%A4%E8%AF%81%E6%8E%88%E6%9D%83%E4%B8%AD%E5%BF%83)
7. [用户注册](https://github.com/Yuki2L0ve/food-social/wiki/7-%E7%94%A8%E6%88%B7%E6%B3%A8%E5%86%8C)
8. [Redis应用之抢购代金券](https://github.com/Yuki2L0ve/food-social/wiki/8-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E6%8A%A2%E8%B4%AD%E4%BB%A3%E9%87%91%E5%88%B8)
9. [Redis应用之好友功能](https://github.com/Yuki2L0ve/food-social/wiki/9-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E5%A5%BD%E5%8F%8B%E5%8A%9F%E8%83%BD)
10. [Redis应用之Feed功能](https://github.com/Yuki2L0ve/food-social/wiki/10-Redis%E5%BA%94%E7%94%A8%E4%B9%8BFeed%E5%8A%9F%E8%83%BD)
11. [Redis应用之签到功能](https://github.com/Yuki2L0ve/food-social/wiki/11-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E7%AD%BE%E5%88%B0%E5%8A%9F%E8%83%BD)
12. [Redis应用之积分功能](https://github.com/Yuki2L0ve/food-social/wiki/12-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E7%A7%AF%E5%88%86%E5%8A%9F%E8%83%BD)
13. [Redis应用之附近的人](https://github.com/Yuki2L0ve/food-social/wiki/13-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E9%99%84%E8%BF%91%E7%9A%84%E4%BA%BA)
14. [Redis应用之缓存餐厅](https://github.com/Yuki2L0ve/food-social/wiki/14-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E7%BC%93%E5%AD%98%E9%A4%90%E5%8E%85)
15. [Redis应用之最新餐厅评论](https://github.com/Yuki2L0ve/food-social/wiki/15-Redis%E5%BA%94%E7%94%A8%E4%B9%8B%E6%9C%80%E6%96%B0%E9%A4%90%E5%8E%85%E8%AF%84%E8%AE%BA)

