# 0013 实现计划

## 切片

- [x] 1. 项目骨架、配置、PostgreSQL/Flyway、Redis、Docker Compose
- [x] 2. Session/Token/ticket 认证和一次性原子消费
- [x] 3. 通知事件、Outbox、补拉和 ACK
- [x] 4. WebSocket 连接、心跳、串行有界发送
- [x] 5. Redis presence、在线用户查询和管理权限
- [x] 6. Outbox relay、Redis Streams 跨实例实时路由
- [x] 7. 浏览器页面、Nginx、指标、README 和故障演练文档
- [x] 8. 测试、Compose 配置检查和最终验证

## 取舍

- PostgreSQL 是可靠事件来源；Redis 只承担短期 ticket、presence 和实时路由。
- 不将在线连接写入 PostgreSQL 作为实时状态，避免把瞬时连接状态做成强一致业务数据。
- 使用 Redis Streams 而不是仅使用 Pub/Sub，避免 Redis 短暂断线直接丢失实时路由；PostgreSQL 补拉仍是最终恢复路径。
- 使用 Spring Data JDBC 和原生 RedisTemplate，减少 ORM 和消息框架依赖。

## 验证记录

- `mvn test`：10 个测试通过；包含 PostgreSQL 17 Testcontainers、Flyway、事务 Outbox、幂等 Key 指纹冲突、ACK/DELIVERED 幂等、Outbox DEAD 状态、Redis ticket Lua 并发消费、连接配额原子申请和 presence TTL/租户隔离。
- `mvn package -DskipTests`：已验证可打包。
- `docker compose config --quiet`：已验证 Compose 配置可解析。
- `docker compose up --build`：已验证 PostgreSQL、Redis、两个应用实例和 Nginx 启动，Flyway 完成迁移。
- Nginx `/actuator/health` 已显式代理；健康检查和会话共享依赖 Redis。
- Review-fix close-out：重新执行 `mvn test`（10 通过）、`mvn package -DskipTests`、`docker compose config --quiet`；执行 `docker compose up -d --build` 后两个 app 均为 `healthy` 且以 `appuser` 运行。
- 运行时检查：`/actuator/health` 返回 200；无效 ticket 返回 401；Nginx 日志只出现 `/ws/notifications`，不包含 query 中的 ticket。

## 未覆盖或已知限制

- 已手工验证 Nginx Upgrade 返回 `101`、ticket 重放返回 `401`、在线连接查询返回实例信息、跨实例通知将 `delivered_at` 写入 PostgreSQL；真实浏览器停机重连和慢客户端仍需按 README 演练。
- Redis Stream 每个实例消费完整事件流再本地过滤，实例数量增大时成本线性增长；大规模部署应按租户或路由分片。
- Presence 索引为租户 Set，过期 key 通过查询清理；超大规模在线目录应使用更专门的租约索引/分页方案。
- Stream 是实时路由缓冲，不是持久化队列；超过 MAXLEN 的实时记录必须依靠 PostgreSQL 补拉。
- `X-Forwarded-*` 只应由受信任的内部 Nginx 注入；生产部署不得直接把应用端口暴露给公网。

## Review-fix 验证记录

- 租户隔离：新增 `PresenceControllerTests`，拒绝请求参数切换到其他租户。
- Redis 故障清理：新增 `NotificationWebSocketHandlerTests`，验证 presence 清理异常时仍关闭 WebSocket 和发送队列。
- 心跳配置：调度器统一读取 `app.heartbeat-interval`，不再使用第二套毫秒环境变量。
- 浏览器重连：ticket 申请失败会沿用指数退避继续申请；脚本 `node --check` 通过。
- `mvn test`：13 个测试通过；`mvn package -DskipTests`、`docker compose config --quiet` 通过。
- 重建 Compose 后 app-a/app-b 均为 `healthy`，`/actuator/health` 返回 200。
