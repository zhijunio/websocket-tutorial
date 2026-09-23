# websocket-tutorial

一个以实践为主的 WebSocket 学习工作区，覆盖 RFC 6455、浏览器 DevTools、Java 25、Spring Boot 4.1.1、Maven、Docker、Jakarta WebSocket、STOMP、MQTT 和性能基准。

课程目标不是只会调用框架 API，而是能够从协议、源码、抓包、日志、测试和指标多个角度理解并排查实时通信系统。

## 在线阅读

如果仓库启用了 GitHub Pages，可以从首页开始：

- [课程首页](index.html)
- [课程列表](index.html#lessons)
- [RFC 6455 参考资料](index.html#references)
- [示例项目索引](examples/index.html)

课程页面和 RFC 参考文档是静态 HTML，可以直接在 GitHub Pages 中阅读。

Java、Spring Boot、Maven 和 Docker 示例需要克隆仓库后在本地运行，GitHub Pages 不负责运行后端服务。

## 内容概览

- WebSocket 与轮询、长轮询的连接模型和资源差异
- RFC 6455 Opening Handshake、101、Upgrade 和 `Sec-WebSocket-*`
- WebSocket 帧、分片、掩码、Ping/Pong 和 Close
- Spring Boot 原生 WebSocket Handler
- Jakarta `@ServerEndpoint` 对照实现
- 多房间聊天、广播和断开清理
- 心跳、断线重连、Origin、鉴权、TLS 和代理超时
- 在线推送、离线未读通知和补拉
- 反向代理、多实例和消息路由
- STOMP over WebSocket
- MQTT Broker 与 WebSocket 客户端桥接
- 高频小消息基准和 p50/p95 延迟分析
- G1/G2/G3 项目验收与 WebSocket 面试演练

## 环境要求

- Java 25
- Maven
- Docker
- 支持 WebSocket 的现代浏览器
- 可选：浏览器 DevTools、Wireshark 或其他抓包工具

主线示例使用 Java 25 和 Spring Boot 4.1.1。每个示例项目都有独立的 Maven 配置，不共享其他示例的构建目录或运行状态。

## 快速开始

运行最小 Spring Boot 示例：

```sh
cd examples/0003-spring-boot-websocket-echo
mvn test
mvn spring-boot:run
```

服务启动后，在浏览器控制台执行：

```js
const socket = new WebSocket("ws://localhost:8080/ws/echo");
socket.onopen = () => socket.send("hello");
socket.onmessage = event => console.log(event.data);
```

预期收到：

```text
echo: hello
```

进入任意示例目录后，通常可以执行：

```sh
mvn test
mvn spring-boot:run
```

需要 Docker 的示例会在对应 README 中说明启动方式，例如：

```sh
docker compose up --build
```

## 示例项目

每个示例都可以独立验证。完整入口见[示例项目索引](examples/index.html)。

| 课程 | 示例目录 | 内容 |
|---|---|---|
| 0003 | `examples/0003-spring-boot-websocket-echo` | Spring WebSocket Handler 回显 |
| 0004 | `examples/0004-jakarta-server-endpoint` | Jakarta `@ServerEndpoint` 回显 |
| 0005 | `examples/0005-multi-room-chat` | 多房间聊天和广播 |
| 0006 | `examples/0006-heartbeat-reconnect-security` | 心跳、重连、Origin 和握手令牌 |
| 0007 | `examples/0007-push-notifications-and-replay` | 在线推送、未读通知和补拉 |
| 0008 | `examples/0008-proxy-and-multi-instance` | Nginx、双实例和跨实例路由 |
| 0009 | `examples/0009-stomp-websocket` | STOMP 帧和 Spring 消息代理 |
| 0010 | `examples/0010-mqtt-websocket-bridge` | MQTT 到 WebSocket 的桥接 |
| 0011 | `examples/0011-high-frequency-benchmark` | 100 客户端性能基准 |

## 课程结构

```text
lessons/       每节课的 HTML 页面
reference/     RFC 6455 和 WebSocket 参考文档
examples/      独立可运行的 Java/Spring Boot 示例
assets/        CSS、JavaScript 和协议架构图
MISSION.md     学习目标、约束和 G1/G2/G3 验收标准
CURRICULUM.md 课程大纲和阶段门槛
RESOURCES.md   外部规范和官方文档
NOTES.md       教学工作区内部维护备注
```

## 学习方式

每节课只聚焦一个核心能力，并尽量包含：

1. 先测后讲的问题
2. 协议或工程核心概念
3. 最小可运行示例
4. 源码路径和关键不变量说明
5. 10 至 30 分钟动手练习
6. 测试、DevTools、抓包、日志或指标验证
7. 一个真实失败案例
8. 面试表达练习

代码型课程要求先运行示例，再阅读关键源码。源码讲解关注入口、状态所有者、并发边界、资源清理和测试证明范围，不进行没有价值的逐行翻译。

## 验收目标

- G1：完成多房间聊天，保证房间隔离、广播正确和断开清理。
- G2：完成在线推送，并支持断线期间的未读标记或补拉。
- G3：使用 100 个客户端、每客户端每秒 10 条、约 256 字节消息、持续 60 秒，重复 3 次，记录吞吐、p50/p95、错误率、CPU 和内存。

`p95 < 200ms` 只是学习基线，不是生产 SLO。

完整目标和约束见 [MISSION.md](MISSION.md)，课程路线见 [CURRICULUM.md](CURRICULUM.md)。

## 参考资料

- [RFC 6455：The WebSocket Protocol](https://www.rfc-editor.org/rfc/rfc6455)
- [RFC 6455 信息页](https://www.rfc-editor.org/info/rfc6455/)
- [MDN WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [Spring WebSocket 官方文档](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP 1.2 规范](https://stomp.github.io/stomp-specification-1.2.html)
- [Jakarta WebSocket 规范](https://jakarta.ee/specifications/websocket/2.2/)

仓库内的中文参考文档从 [RFC 6455 总导读](reference/rfc6455-guide.html) 开始。

## GitHub Pages

根目录的 `index.html` 可作为 GitHub Pages 首页。发布前确认：

1. 仓库已经推送到 GitHub。
2. GitHub Pages 的发布源选择包含 `index.html` 的分支或目录。
3. 页面中的相对链接仍然可以访问。
4. Java 示例只作为源码和文档展示，实际运行需要本地 Java、Maven 或 Docker 环境。

## 维护与贡献

这是一个学习型项目。修改课程或示例时，请保持以下约定：

- 优先使用 RFC、官方文档、DevTools、抓包、日志和可重复测试作为依据。
- 区分 WebSocket 标准、浏览器行为、Spring/Tomcat 行为、代理行为和业务实践。
- 示例项目保持独立可运行。
- 新增代码时补充启动命令、验证方式和必要测试。
- 不提交构建产物、日志、凭证或其他敏感信息。
- 保持最小依赖、最小抽象和清晰目录结构。

目前仓库没有单独的贡献者名单或发布流程；改进课程内容可以通过提交 Issue 或 Pull Request 讨论。
