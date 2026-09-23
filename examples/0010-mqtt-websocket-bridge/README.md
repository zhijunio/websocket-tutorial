# MQTT 到 WebSocket 桥接

第 0010 课的独立示例。

## 环境要求

- Java 25
- Maven
- Docker

## 使用 Mosquitto 运行

```sh
mvn test
docker compose up --build
```

桥接服务提供 `ws://localhost:8080/ws/devices?device-1` 和 HTTP 命令端点：

```sh
curl -X POST http://localhost:8080/api/devices/device-1/commands \
  -H 'Content-Type: application/json' \
  -d '{"payload":"restart"}'
```

发布 MQTT 事件前，先连接浏览器 WebSocket：

```js
const socket = new WebSocket("ws://localhost:8080/ws/devices?device-1");
socket.onmessage = event => console.log(event.data);
```

在另一个终端发布 MQTT 事件：

```sh
docker compose exec mqtt mosquitto_pub \
  -h localhost -t devices/device-1/events -q 1 -m '{"temperature":21.5}'
```

浏览器会通过 Java 桥接服务收到 MQTT 负载。

## 各组件职责

- MQTT Broker 传输面向设备的主题和 QoS 语义。
- Java 桥接服务订阅 `devices/+/events`，解析设备 ID，并把负载发送给对应的 WebSocket 会话。
- WebSocket 向浏览器提供实时连接。
- HTTP 向 `devices/{deviceId}/commands` 发布命令。

代码直接使用 Paho，使连接和回调过程清晰可见。在更大的 Spring 应用中，Spring Integration MQTT 可以为同一边界提供入站/出站适配器和消息通道。但它不会替你设计主题映射、QoS、重试、授权、背压和离线语义。

本示例允许匿名 MQTT 访问，并将 WebSocket 会话保存在单个 JVM 中，仅用于本地学习。
