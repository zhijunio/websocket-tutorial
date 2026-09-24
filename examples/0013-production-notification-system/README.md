# 0013 生产导向实时通知系统

本示例把前置课程的 WebSocket 能力组合成一个通知系统：服务端产生告警或工单事件，按租户和用户实时推送；客户端确认消息，断线后按 `sequence` 补拉。

它不是聊天室：没有房间、群聊、成员列表或聊天历史。WebSocket 只负责实时传输，PostgreSQL 才是通知事件和 Outbox 的可靠来源。

## 运行

本项目固定使用 PostgreSQL，不使用 H2。

本地运行应用需要准备 PostgreSQL 和 Redis，然后设置：

```sh
export DATABASE_PASSWORD='local-password'
export JWT_SECRET='a-long-development-secret-at-least-32-bytes'
mvn test
mvn spring-boot:run
```

完整多实例环境：

```sh
export DATABASE_PASSWORD='local-password'
export JWT_SECRET='a-long-development-secret-at-least-32-bytes'
mvn package -DskipTests
docker compose config
docker compose up --build
```

访问 <http://localhost:8080/>。Compose 中 `app-a` 和 `app-b` 使用同一个 PostgreSQL/Redis，Nginx 负责 HTTP 和 WebSocket Upgrade 转发。

## 认证

演示账号为 `alice/password`、`bob/password` 和 `admin/password`。

- Session：先调用 `/api/session/login`，浏览器保存 Session Cookie，连接 `/ws/notifications`。Session 存储在 Redis，因此 Nginx 把后续 HTTP 请求分配到另一应用实例时仍可认证；生产环境应打开 `SESSION_COOKIE_SECURE=true` 并使用 HTTPS。
- Token：调用 `/api/token` 获取 Bearer Token，再调用 `/api/ws-ticket`。ticket 是 Redis 中的高熵不透明值，默认 30 秒有效，只能原子消费一次，并绑定用户、租户、客户端和 WebSocket 用途。
- 浏览器原生 WebSocket 不能设置 `Authorization` Header，因此浏览器使用 ticket；长期 Token 不放入 URL。
- 每次重连都重新申请 ticket。ticket 过期、被消费或握手失败时不能复用。

生产环境必须使用独立身份服务、HTTPS/WSS、密钥管理、限流和安全审计。示例账号和环境变量默认值只用于本地教学，不能用于生产。

## 消息协议

客户端消息：

```json
{"type":"ping","messageId":"client-message-id"}
{"type":"resume","after":100}
{"type":"ack","eventId":"event-uuid"}
```

服务端通知：

```json
{
  "type": "notification",
  "messageId": "message-id",
  "eventId": "event-uuid",
  "sequence": 101,
  "eventType": "ALARM_CREATED",
  "payload": {"text": "测试告警"}
}
```

客户端 `resume` 会从 PostgreSQL 补拉大于指定 sequence 的当前用户事件。ACK 是幂等的，重复 ACK 不会产生额外副作用。

创建通知必须携带 `Idempotency-Key` 请求头；同一租户重复使用该 Key 且请求指纹相同会返回原事件，不会重复写 Outbox 或投递记录。相同 Key 携带不同接收者或 payload 会返回 `409 Conflict`，避免把调用方错误静默当成成功。生产调用方应为一次业务请求稳定生成 Key，并在重试时复用它。

## 数据和可靠性

Flyway 创建：

- `notification_event`：租户、接收者、事件 payload、全局 sequence 和租户内幂等 Key。
- `notification_outbox`：与事件同事务写入，使用 claim/lease、重试和退避发布到 Redis Stream。
- `notification_delivery`：投递和 ACK 状态。

发布流程：

```text
HTTP 业务请求
    ↓ 同一 PostgreSQL 事务
notification_event + notification_outbox
    ↓ relay claim/lease
Redis Stream（每个实例一个 consumer group）
    ↓
目标实例的本地 WebSocket 连接
    ↓ 断线时
PostgreSQL sequence 补拉
```

Redis Stream 只负责低延迟实时转发。实例或 Redis 短暂故障时，事件仍在 PostgreSQL；Outbox 会重试，客户端重连也可以补拉。Redis Pub/Sub 不具备离线补偿能力，因此本示例没有把 Pub/Sub 当作唯一可靠机制。

Stream 使用近似 `MAXLEN` 保留策略（默认 100000 条），防止无限增长；它不是可靠存储，保留窗口必须结合 PostgreSQL 补拉 SLA 配置。

语义是“持久化事件 + 至少一次实时转发 + 客户端/服务端幂等”。WebSocket 本身不提供可靠消息语义。

## 在线用户查询

每个连接在 Redis 中保存带 TTL 的 presence：

```text
ws:presence:{tenantId}:{userId}:{connectionId}
```

字段包括 `tenantId`、`userId`、`clientType`、`connectionId`、`instanceId`、`connectedAt` 和 `lastSeenAt`。心跳默认 15 秒，presence 租约默认 45 秒。关闭连接只删除自己的 connectionId，实例崩溃后由 TTL 自动过期。

在线状态是最终一致的临时状态，不是 PostgreSQL 强一致事实。查询接口使用 Redis SCAN，不使用 `KEYS`，并且需要管理员权限：

连接建立时还会通过 Redis Lua 一次性检查并申请用户、租户、实例和 IP 四类连接租约；心跳续租，关闭时只删除自己的 connectionId。这样多实例并发握手不会绕过配额。

在线查询的租户范围由服务端当前认证上下文决定，不能通过请求参数切换到其他租户。

```text
GET /api/presence/users
GET /api/presence/users/{userId}
GET /api/presence/connections
```

## 连接和慢客户端

每条连接使用有界 `ArrayBlockingQueue` 和单独的虚拟线程串行发送。Redis listener 不直接写 WebSocket；队列满、连接关闭或发送异常会移除连接。这样一个慢客户端不会阻塞其他连接，但实时通知仍可能在队列溢出时被丢弃，客户端随后通过 sequence 补拉。

真实生产环境还应把队列深度、发送延迟、关闭原因和丢弃数接入 Micrometer，并根据容量压测设置连接数和队列上限。Redis 清理异常时本地连接仍会关闭，Redis 租约依靠 TTL 回收。

## 反向代理和超时

Nginx 转发：

- `proxy_http_version 1.1`。
- `Upgrade` 和 `Connection: upgrade`。
- `proxy_read_timeout 90s`。

代理空闲超时不是心跳。生产环境应保证：心跳周期小于代理空闲超时，presence 租约大于心跳周期，并为滚动发布设置连接排空和 `server_shutdown` 通知。

## 安全边界

- Origin 使用白名单，禁止 `*`。
- Session Cookie 应启用 Secure、HttpOnly、合适的 SameSite。
- Session 写操作使用 CSRF Token；Token 和 ticket 接口使用明确的无 Cookie 认证策略。
- ticket 原子消费使用 Redis Lua `GET + DEL`。
- Nginx access log 只记录 `$uri`，不记录包含 ticket 的查询参数；应用通过受控反向代理转发的真实客户端地址执行握手/IP 限流。
- 容器应用使用非 root 用户，并配置健康检查和优雅停机。
- 管理在线查询需要 ADMIN 权限。
- 不记录 Token、ticket、Cookie、签名、完整认证 URL 或未脱敏 payload。
- 示例中的 JWT 默认值和密码必须在生产部署中替换。

## 测试和故障演练

测试使用 Testcontainers 验证 PostgreSQL、Redis、Flyway、Outbox 和 Redis Lua ticket 原子消费；不使用 H2 代替 PostgreSQL。当前测试环境需要 Docker API 兼容 Testcontainers 2.x。

建议演练：

1. 连接落在 `app-a`，通知请求到 `app-b`，仍能收到通知。
2. 停止连接所在实例，浏览器重新申请 ticket 并连接另一实例。
3. Redis 暂停后恢复，Outbox 重新发布。
4. 断线期间产生通知，重连后 `resume` 补拉。
5. 同一个 ticket 重放第二次失败。
6. PostgreSQL 暂停后恢复，已提交事件不静默丢失。
7. 模拟慢客户端，确认不会阻塞其他连接。
8. 检查日志不包含 Token、ticket 和 Cookie。

## 与其他方案的边界

- 原生 Spring WebSocket：本示例选择的低抽象方案，业务消息协议和连接背压由应用负责。
- STOMP：适合需要标准目的地、订阅和 broker 语义的应用，但会增加协议和 broker 配置层。
- MQTT：适合设备、topic、QoS 和 broker 场景，不是浏览器用户通知的默认替代品。
- Jakarta `@ServerEndpoint`：更接近标准 API；本仓库前置课程用它比较容器模型，本示例需要 Spring Security、事务和 Redis 集成，因此使用 Spring Handler。

## 已知限制

示例已经覆盖生产设计的关键边界，但不能替代真实生产的容量评估、灾备、密钥轮换、完整授权模型、数据库高可用和安全审计。连接握手按 IP 限流，并限制实例、租户、用户和 IP 连接数；通知消息和 Outbox 都有配置上限。Redis presence 的查询使用租户索引和分页，仍是最终一致状态；Redis Stream 的消费组和 PostgreSQL 补拉需要结合业务 SLA 继续配置保留、重试和死信策略。`X-Forwarded-*` 只应由受信任的内部 Nginx 注入，生产部署不得直接把应用端口暴露给公网。
