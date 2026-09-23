# 反向代理与多实例路由

第 0008 课的独立示例。

## 环境要求

- Java 25
- Maven
- Docker

## 运行

```sh
mvn test
docker compose up --build
```

Nginx 代理监听 `http://localhost:8080`。

检查哪个后端响应 HTTP 请求：

```sh
curl http://localhost:8080/api/instance
```

通过代理建立 WebSocket 连接：

```js
const socket = new WebSocket("ws://localhost:8080/ws/notifications?alice");
socket.onmessage = event => console.log(event.data);
```

Nginx 使用 HTTP/1.1 转发 HTTP Upgrade 相关请求头。`proxy_read_timeout` 是基础设施的空闲超时，不是 WebSocket 心跳。

## 多实例失败场景

连接保存在每个进程本地的 `ConnectionRegistry` 中。如果 Alice 的 WebSocket 连接在 `app-a`，但发布请求到达 `app-b`，则 `deliveredLocally` 为 `false`。生产设计需要共享消息总线、连接目录、明确说明限制的会话粘滞策略，或能够将发布请求路由到连接所属节点的网关。

本示例会把缺少路由的问题显式暴露出来；两个进程加 Nginx 并不会自动解决水平扩展问题。
