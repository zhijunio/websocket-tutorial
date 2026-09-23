# Mission: WebSocket

## Why

要在公司场景里能做实时推送和聊天，在面试中讲清协议与实现选型，并能在个人项目里快速落地 demo。目标不仅是会写 Spring/Jakarta 代码，也能在握手失败、断线、网关超时等问题上用 RFC、抓包和日志排错，而不是只调框架注解。

## Success looks like

- 能用自己的话说明 WebSocket 与反复 HTTP 在连接模型、延迟和资源上的差异。
- 能读懂 RFC 6455 的 Opening Handshake，并在浏览器 DevTools 或抓包中认出 Upgrade、101 和 `Sec-WebSocket-*`。
- 能用 Spring Boot 跑通双向消息 demo，并说明同一需求在 Jakarta `@ServerEndpoint` 下如何实现。
- 能对比裸 WebSocket、STOMP、SockJS 各自解决的问题，并说明心跳、Origin、鉴权和会话扩展的常见做法。
- 能结构化回答附录 E 中标 ⭐ 的高频题；能用 G1/G2/G3 各讲 2 分钟“问题—方案—权衡”；能白板画握手与水平扩展。
- 能说清 WebSocket 与 MQTT 的分工，画出 MQTT Broker ↔ Java 后端 ↔ WebSocket 客户端桥接，并知道 Spring Integration MQTT / Paho 的常见角色。
- （进阶）能说明 Netty 在管线中承担什么，何时值得自研而非只用 Spring/Tomcat。

## Constraints

- 主线使用 Java + Spring Boot；用 Jakarta `@ServerEndpoint` 做对照。
- 以 RFC、官方文档、DevTools、抓包和日志为主要依据。
- 每课尽可能详细，并在适合时使用时序图、状态图或架构图；但每课只聚焦一个核心能力。
- 不要求自建集群级 MQTT Broker 运维；优先性能，其次成本、复杂度和形式感。
- 主线环境以 Java 25、Spring Boot 4.1.1、Maven 和 Docker 为基线。

## Out of scope

- 暂不深入自建 MQTT 集群运维、完整 IM 离线消息系统、WebRTC、SSE 深入实现、复杂前端框架和 Netty 源码级学习。

## §4 结业验收

- **G1 多房间聊天**：支持多个房间、加入/离开和房间内广播；不同房间不串消息；断开后成员状态最终清理。
- **G2 推送通知**：支持在线推送，并对断线期间的通知提供未读标记或补拉机制；能说明多实例下消息如何到达目标连接。
- **G3 高频小消息**：在本地用 100 个客户端、每客户端每秒 10 条、约 256 字节消息、持续 60 秒的负载重复 3 次；记录吞吐、p50/p95 延迟、错误率、CPU 和内存，并解释优化前后的瓶颈与权衡。`p95 < 200ms` 只是学习基线，不是生产 SLO。
- 三项项目都必须有可运行代码、README、架构图、消息协议和故障排查说明。
- 每个检查点都必须有单元/集成测试、启动和验证命令、关键日志或指标，以及至少一个失败案例；敏感凭证不得进入日志。
- 面试验收：每个项目都能用 2 分钟讲清“问题—方案—权衡”，并能白板画握手与水平扩展。

## 附录 E：高频面试题

- ⭐ WebSocket 与轮询/长轮询的连接模型、延迟和资源差异是什么？
- ⭐ Opening Handshake 中 `Upgrade`、`Connection`、`Sec-WebSocket-Key`、`Sec-WebSocket-Accept` 和 `101` 分别做什么？
- ⭐ 为什么 `Sec-WebSocket-Key` 不是身份认证？WebSocket 鉴权放在哪里做？
- ⭐ 握手返回 `200`、`401`、`404` 或代理超时时，如何用抓包和日志定位？
- ⭐ Ping/Pong、业务心跳、TCP keepalive 和代理 idle timeout 有什么区别？
- ⭐ WebSocket 如何做 Origin 校验、断线重连和多实例水平扩展？
- ⭐ 裸 WebSocket、STOMP、SockJS 分别解决什么问题，如何选型？
- ⭐ WebSocket 与 MQTT 如何分工？什么时候需要 MQTT Broker ↔ Java 后端 ↔ WebSocket 的桥接？
- ⭐ Spring/Tomcat 已经够用时，为什么还要或不该引入 Netty？
