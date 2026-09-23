# WebSocket 上的 STOMP

第 0009 课的独立示例。

## 环境要求

- Java 25
- Maven

## 运行

```sh
mvn test
mvn spring-boot:run
```

打开 <http://localhost:8080/>。在两个浏览器标签页中依次点击 **Connect**、**Subscribe** 和 **Send**。

浏览器使用承载 STOMP 帧的原生 WebSocket 帧：

```text
CONNECT
accept-version:1.2
host:localhost:8080

^@
```

服务端回复 `CONNECTED`。订阅 `/topic/room` 后，可以收到发送到 `/app/chat.send` 的消息；Spring 会将应用目的地映射到 `ChatMessageController.send`，再把返回值发布到简单代理目的地。

本示例使用 Spring 的内存简单代理，仅适用于学习和单进程场景；生产多实例部署需要 Broker Relay 或其他共享路由设计。`setAllowedOriginPatterns("*")` 仅是本地示例配置。
