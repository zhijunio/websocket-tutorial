# 推送通知与离线补拉

第 0007 课的独立示例，也是 G2 的起点实现。

## 环境要求

- Java 25
- Maven

## 运行

```sh
mvn test
mvn spring-boot:run
```

## 示例流程

为用户 `alice` 连接一个浏览器客户端：

```js
const socket = new WebSocket("ws://localhost:8080/ws/notifications?alice");
socket.onmessage = event => console.log(JSON.parse(event.data));
```

在 Alice 在线时发布通知：

```sh
curl -X POST http://localhost:8080/api/users/alice/notifications \
  -H 'Content-Type: application/json' \
  -d '{"text":"build finished"}'
```

浏览器会立即收到 WebSocket 通知。关闭浏览器连接后再发布一条通知，该通知会被保存为未读：

```sh
curl http://localhost:8080/api/users/alice/notifications/unread
```

客户端重新连接后，示例不会自动补拉。请获取未读列表、显示通知并确认其中一条：

```sh
curl -X POST http://localhost:8080/api/users/alice/notifications/1/ack
```

## 重要限制

这是单进程内存学习示例。它没有持久化存储、认证、授权、多实例路由、投递事务或跨进程去重。查询字符串只是选择示例用户的简便方式，不构成认证。
