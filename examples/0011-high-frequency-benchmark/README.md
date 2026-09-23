# 高频 WebSocket 基准测试

第 0011 课和 G3 的独立示例。

## 环境要求

- Java 25
- Maven

## 运行服务端

```sh
mvn test
mvn spring-boot:run
```

回显端点为 `ws://localhost:8080/ws/load`。

## 运行 G3 基线

在第二个终端执行：

```sh
mvn -q exec:java \
  -Dexec.mainClass=com.example.websocket.LoadTestRunner \
  -Dexec.args="100 10 60 ws://localhost:8080/ws/load"
```

参数依次为 `clients messagesPerSecond durationSeconds endpoint`。默认基线为 100 个客户端、每个客户端每秒 10 条消息、持续 60 秒、文本负载 256 字节。请使用完全相同的命令重复运行三次。

运行器会报告发送数、接收数、丢失数、未完成数、吞吐量、错误率以及 p50、p95 往返延迟。它会按目标总消息数收敛发送，并在关闭连接前等待回显或超时。延迟测量的是从客户端提交 `sendText` 到收到回显的时间，不是服务端处理时间。

## 服务端资源测量

请在独立进程或容器中运行服务端，并在负载运行期间记录 CPU 和内存。例如使用容器时：

```sh
docker stats --no-stream <container>
```

记录 Java 版本、机器信息、JVM 参数、服务端日志、预热状态、测试次数和错误信息。`p95 < 200ms` 是 Mission 中的学习基线，不是生产 SLO。

## 范围与限制

这是一个回显基准测试，不模拟房间广播、持久化、认证、TLS、反向代理或 Broker。该运行器也不能替代生产级负载生成器；它被刻意保持为易于在课程中阅读和修改的小工具。
