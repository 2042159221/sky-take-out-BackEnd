# 苍穹外卖项目（后端）

## 项目介绍

苍穹外卖是一个基于Spring Boot的餐饮外卖系统，采用前后端分离架构，包含管理端和用户端。本仓库为项目后端代码部分。

前端仓库：[sky-take-out-FrontEnd](https://github.com/2042159221/sky-take-out-FrontEnd)

## 技术栈

- 后端框架：Spring Boot
- ORM框架：MyBatis
- 数据库分页：PageHelper
- 数据库：MySQL
- 缓存：Redis
- 前端框架：Vue.js（管理端）、微信小程序（用户端）

## 项目结构

```
sky-take-out/
├── sky-common/ -- 公共模块
│   ├── 常量定义
│   ├── 异常处理
│   ├── 工具类
│   └── 通用响应封装
├── sky-pojo/ -- 实体类模块
│   ├── entity -- 实体类
│   ├── dto -- 数据传输对象
│   └── vo -- 视图对象
└── sky-server/ -- 主模块
    ├── controller -- 控制器
    ├── service -- 业务逻辑
    ├── mapper -- 数据访问
    ├── aspect -- 切面
    ├── config -- 配置类
    ├── interceptor -- 拦截器
    └── websocket -- WebSocket处理
```

## 主要功能

- 管理端
  - 员工管理
  - 分类管理
  - 菜品管理
  - 套餐管理
  - 订单管理
  - 数据统计

- 用户端
  - 微信登录
  - 地址管理
  - 菜品浏览
  - 购物车
  - 订单管理
  - 支付功能

## 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

## 使用说明

### 项目启动

1. 克隆项目到本地
2. 创建数据库，执行SQL脚本
3. 修改`application.yml`中的数据库连接信息
4. 执行`mvn clean package`打包项目
5. 运行`java -jar sky-server/target/sky-server.jar`启动项目

### 接口文档

启动项目后，访问：http://localhost:8080/doc.html 查看接口文档

## 许可证

[MIT License](LICENSE) 