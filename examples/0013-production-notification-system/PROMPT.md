# 0013 生产导向实时通知系统：实现规格

请在当前 websocket-tutorial 仓库中新增一个完整的 WebSocket 综合示例，课程编号为 0013。

示例名称：`0013-production-notification-system`。

业务场景是告警、工单、支付状态等通知推送，不是聊天室。禁止实现多房间、群聊、聊天成员列表和聊天历史。

## 技术要求

- Java 25、Spring Boot 4.1.1、Maven。
- PostgreSQL，禁止使用 H2。
- Redis、Nginx、Docker Compose、Flyway。
- Spring WebSocket、Spring Security、Spring Data JDBC、Actuator/Micrometer。
- 两个应用实例：8081 和 8082，由 Nginx 统一代理。
- 密码、JWT 密钥、ticket 密钥只能来自环境变量或 Docker Secret。
- 示例必须支持 `mvn test`、`docker compose config` 和 `docker compose up --build`。

## 核心架构

- PostgreSQL 保存通知事件、Outbox、投递/ACK 状态和必要的审计记录。
- Redis 保存一次性 WebSocket ticket、带 TTL 的跨实例在线连接目录和实例级实时队列。
- PostgreSQL Outbox 与业务事件在同一事务中提交；relay 负责重试和发布。
- Redis Pub/Sub 不能作为唯一可靠投递机制。实时转发失败时，客户端重连后从 PostgreSQL 补拉。
- WebSocket 端点为 `/ws/notifications`，只负责实时传输，不承担可靠存储。

## 认证

同时保留两种认证路径：

1. Session：HTTP 登录后建立 Session，握手携带 Cookie，服务端从 Spring Security Principal 获取身份。
2. Token + ticket：浏览器用 Bearer Token 请求 `/api/ws-ticket`，获取 30 秒有效、高熵、绑定用户/租户/客户端/用途的 Redis ticket。ticket 只能原子消费一次，只用于一次握手；每次重连重新申请。

长期 Token 不得放入 WebSocket URL。ticket、Token、签名、Cookie 和完整认证 URL 不得写日志。

## HTTP API

- `POST /api/session/login`
- `POST /api/session/logout`
- `POST /api/token`
- `POST /api/ws-ticket`
- `POST /api/notifications`
- `GET /api/notifications?after={sequence}`
- `POST /api/notifications/{eventId}/ack`
- `GET /api/presence/users`
- `GET /api/presence/users/{userId}`
- `GET /api/presence/connections`

通知创建、补拉和 ACK 必须校验租户、用户和事件权限，并保证幂等。

## WebSocket 协议

客户端消息：`ping`、`resume`、`ack`、`close`。

服务端消息：`pong`、`notification`、`replay`、`error`、`auth_expired`、`server_shutdown`。

消息至少包含 `type`、`messageId`、`timestamp`，通知还包含 `eventId` 和 `sequence`。

## 连接和在线状态

- 支持同一用户多个连接。
- 本地连接表线程安全；连接关闭、传输异常和发送失败都必须清理。
- 每连接使用有界发送队列和串行发送，慢客户端不能阻塞其他连接或 Redis listener。
- Redis presence 记录 tenantId、userId、clientType、connectionId、instanceId、connectedAt、lastSeenAt 和 TTL。
- 连接租约默认 45 秒，心跳默认 15 秒。
- 连接关闭只删除自己的 connectionId，实例崩溃后状态依靠 TTL 自动过期。
- 在线查询过滤过期连接，禁止使用 Redis `KEYS`。
- 在线查询接口必须管理员授权，支持租户、用户、客户端、实例和分页过滤。

## 心跳、重连和安全

- 支持 WebSocket Ping/Pong 和应用层 ping/pong。
- Origin 使用白名单，不允许 `*`。
- 浏览器使用指数退避、随机抖动、最大重连间隔和手动停止。
- 每次重连重新申请 ticket。
- 实现连接数、消息大小、握手和通知限流。
- 配置 WSS、Secure/HttpOnly/SameSite Cookie、CSRF 策略和优雅停机。

## 可靠性和测试

- 事件与 Outbox 同事务提交。
- Outbox 支持租约、重试、退避和失败状态。
- 断线补拉、至少一次实时投递、客户端/服务端幂等、sequence 顺序语义必须在 README 中说明。
- 使用 Testcontainers 测试 PostgreSQL 和 Redis，禁止用 H2 替代。
- 测试握手认证、Origin、ticket 单次消费/并发消费、连接清理、多连接、跨租户隔离、Outbox 重试、Redis 故障、断线补拉、ACK 幂等、慢客户端、presence TTL、跨实例查询和管理接口权限。

## 文档和演练

README 必须说明架构、表结构、Redis 数据结构、认证、ticket、在线目录、多实例路径、Outbox、ACK、补拉、慢客户端、代理超时、优雅停机、指标、故障排查和与 STOMP/MQTT/Jakarta WebSocket 的对比。

至少演练：连接跨实例投递、实例故障重连、Redis 暂停恢复、断线补拉、ticket 重放/过期、PostgreSQL 暂停恢复、慢客户端隔离和敏感日志检查。

## 验收

执行并记录：

```sh
mvn test
docker compose config
docker compose up --build
```

最终输出修改文件、架构说明、数据库和 Redis 设计、协议、认证、在线查询、多实例路径、测试和故障演练结果、已知限制及容量优化建议。
