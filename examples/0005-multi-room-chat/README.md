# 多房间 WebSocket 聊天

第 0005 课的独立示例，也是 G1 的第一个实现。

## 环境要求

- Java 25
- Maven

## 运行

```sh
mvn test
mvn spring-boot:run
```

WebSocket 端点为 `ws://localhost:8080/ws/chat`。

## 消息协议

客户端命令：

```json
{"type":"join","room":"red"}
{"type":"leave","room":"red"}
{"type":"chat","room":"red","text":"hello"}
```

服务端事件包括：

```json
{"type":"joined","room":"red","members":1}
{"type":"message","room":"red","from":"session-id","text":"hello"}
{"type":"error","code":"not_joined","message":"..."}
```

房间名长度为 1 到 32 个字符，只允许字母、数字、`_` 或 `-`。聊天文本长度为 1 到 256 个字符。

## 浏览器冒烟测试

打开两个浏览器标签页，分别在控制台中运行：

```js
const socket = new WebSocket("ws://localhost:8080/ws/chat");
socket.onmessage = event => console.log(JSON.parse(event.data));
socket.onopen = () => socket.send(JSON.stringify({type: "join", room: "red"}));
```

两个标签页都加入房间后，在其中一个标签页发送聊天命令：

```js
socket.send(JSON.stringify({type: "chat", room: "red", text: "hello"}));
```

`red` 房间中的两个标签页都应该收到消息。位于 `blue` 房间的标签页不能收到该消息。

这是学习用途的实现。房间成员关系保存在内存中，允许所有本地 Origin，并且不提供鉴权、持久化、投递确认或多实例路由。
