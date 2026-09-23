# Spring Boot WebSocket 回显

第 0003 课的独立示例。

## 环境要求

- Java 25
- Maven

## 运行

```sh
mvn test
mvn spring-boot:run
```

WebSocket 端点为 `ws://localhost:8080/ws/echo`。

在浏览器控制台中验证：

```js
const socket = new WebSocket("ws://localhost:8080/ws/echo");
socket.onopen = () => socket.send("hello");
socket.onmessage = event => console.log(event.data);
```

预期响应：`echo: hello`。

`setAllowedOriginPatterns("*")` 仅用于本地学习。生产环境使用前应限制可信 Origin。
