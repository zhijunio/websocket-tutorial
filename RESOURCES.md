# WebSocket Resources

## Knowledge

- [RFC 6455: The WebSocket Protocol](https://www.rfc-editor.org/rfc/rfc6455)
  IETF 标准，作为握手、帧、关闭和协议语义的最终依据；遇到“框架到底做了什么”时查这里。
- [RFC 6455 信息页](https://www.rfc-editor.org/info/rfc6455/)
  RFC Editor 的元数据、状态和勘误入口；配套参考文档为 `reference/rfc6455-guide.html`，并按章节拆分保存在 `reference/`。
- [MDN: WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
  浏览器端 API 的可靠参考；用于练习客户端连接、事件、发送和关闭。
- [Spring Framework: WebSocket Support](https://docs.spring.io/spring-framework/reference/web/websocket.html)
  Spring 官方文档；后续用于比较原生 WebSocket、STOMP 和 SockJS 的职责边界。
- [Spring Framework: STOMP over WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
  Spring 官方 STOMP 文档；用于学习 Message Broker、目的地映射、用户目的地、心跳和 broker relay。
- [STOMP Specification 1.2](https://stomp.github.io/stomp-specification-1.2.html)
  STOMP 帧、命令、订阅、确认和事务语义的协议依据。
- [Eclipse Paho MQTT Java](https://github.com/eclipse-paho/paho.mqtt.java)
  Java MQTT 客户端实现；用于理解客户端连接、订阅、发布和重连。
- [Spring Integration MQTT](https://docs.spring.io/spring-integration/reference/mqtt.html)
  Spring Integration 的 MQTT inbound/outbound adapter 和消息通道参考。
- [Jakarta WebSocket Specification](https://jakarta.ee/specifications/websocket/2.2/)
  `@ServerEndpoint` 编程模型的规范入口；用于理解它与 Spring 编程模型的差异。

## Wisdom (Communities)

- [Spring Framework Issue Tracker](https://github.com/spring-projects/spring-framework/issues)
  Spring 官方项目社区入口；用于检索 WebSocket、代理、Origin、断线和框架行为，提问或提交问题前应附握手请求/响应和最小复现。

## Gaps

- 生产级 MQTT 集群运维、桥接故障恢复和水平扩展仍只做概念与本地实验，不进入集群运维深度。
