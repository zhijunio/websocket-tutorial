# 示例项目

每个目录都是对应课程的独立示例项目。

| 课程 | 示例目录 | 用途 |
|---|---|---|
| 0003 | `0003-spring-boot-websocket-echo` | Spring WebSocket Handler 回显端点 |
| 0004 | `0004-jakarta-server-endpoint` | Jakarta `@ServerEndpoint` 回显端点 |
| 0005 | `0005-multi-room-chat` | 支持房间隔离和清理的多房间聊天 |
| 0006 | `0006-heartbeat-reconnect-security` | 应用层心跳、重连、Origin 和握手令牌 |
| 0007 | `0007-push-notifications-and-replay` | 在线推送、离线未读通知和补拉 |
| 0008 | `0008-proxy-and-multi-instance` | 两个服务实例、反向代理和跨实例通知路由失败演示 |
| 0009 | `0009-stomp-websocket` | STOMP 帧、Spring 消息代理和主题路由 |
| 0010 | `0010-mqtt-websocket-bridge` | MQTT Broker 到 Java 后端再到 WebSocket 的桥接 |
| 0011 | `0011-high-frequency-benchmark` | 100 客户端 WebSocket 负载测试和延迟指标 |

进入示例目录后运行：

```sh
mvn test
mvn spring-boot:run
```
