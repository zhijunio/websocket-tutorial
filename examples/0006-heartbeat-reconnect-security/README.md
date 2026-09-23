# 心跳、重连与安全边界

第 0006 课的独立示例。

## 环境要求

- Java 25
- Maven

## 运行

```sh
mvn test
mvn spring-boot:run
```

打开 <http://localhost:8080/> 并点击 **Connect**。页面每 5 秒发送一次应用层心跳：

```json
{"type":"ping","id":"client-generated-id"}
```

服务端回复：

```json
{"type":"pong","id":"client-generated-id"}
```

服务端会关闭空闲超过 15 秒的会话。浏览器使用带抖动的指数退避策略重连。

## 安全边界

- 服务端检查 `Origin: http://localhost:8080`。
- 握手还要求携带 `token=local-dev-token`。
- 查询字符串令牌只是为了让浏览器示例自包含。查询参数可能通过日志和 URL 泄露；生产系统应优先使用安全会话 Cookie 或短期连接票据。
- 该令牌只能证明示例客户端提供了预期值，不能代替完整的用户认证或授权设计。

允许的 Origin 和令牌是有意设置的本地示例配置。生产使用前必须替换它们。
