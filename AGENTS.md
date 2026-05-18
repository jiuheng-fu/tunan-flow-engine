# tunan-flow-engine 后端协作说明

## 项目定位

`tunan-flow-engine` 是 `tunan` 项目的后端流程引擎，基于 Spring Boot。它负责流程定义管理、流程发布、动态网关注册、流程执行，以及对前端 `flow-designer` 提供接口。

## 技术栈

- Java 17
- Spring Boot 3.5.4
- Maven
- Spring MVC / WebFlux / WebSocket
- PostgreSQL
- MyBatis-Plus
- Redis 依赖已引入
- Caffeine 本地缓存
- Lombok
- Hutool
- Guava

## 常用命令

```sh
mvn spring-boot:run
mvn test
```

## 运行配置

配置文件：`src/main/resources/application.yml`

关键配置：

- 服务端口：`3000`
- 应用名：`flow-engine`
- 数据库：`jdbc:postgresql://localhost:5433/flow_ai_engine?stringtype=unspecified`
- 数据库用户：`postgres`
- 数据库密码：`postgres123`
- MyBatis-Plus 开启下划线转驼峰
- 开发环境开启 SQL 日志：`org.apache.ibatis.logging.stdout.StdOutImpl`

前端 `flow-designer` 默认通过 `/api` 代理访问本服务，并去掉 `/api` 前缀。

## 目录重点

- `src/main/java/com/tunan/flow/TunanFlowEngineApplication.java`：Spring Boot 启动入口。
- `controller/FlowController.java`：流程定义 CRUD 和发布接口。
- `controller/McpController.java`：MCP 相关接口。
- `service/FlowService.java`：流程定义服务接口。
- `service/impl/FlowServiceImpl.java`：流程定义保存、加载、更新、删除实现。
- `service/FlowPublishService.java`：流程发布、取消发布、重新发布。
- `engine/FlowExecutor.java`：流程执行核心。
- `engine/ExecutionContext.java`：流程执行上下文。
- `engine/component/ComponentExecutor.java`：所有节点执行器接口。
- `engine/component/http/HttpComponentExecutor.java`：HTTP 节点执行器。
- `engine/component/gateway`：入口类节点执行器。
- `engine/gateway`：REST、流式、WebSocket、MCP 等动态网关注册和处理。
- `entity/FlowDefinition.java`：流程定义实体，对应表 `flow_definition`。
- `dto`：流程定义、节点、连线、接口配置、执行结果等 DTO。
- `common/Result.java`：统一响应包装。
- `exception`：全局异常处理。

## 核心接口

`FlowController` 基础路径：`/flow`

- `GET /flow/list`：获取流程列表
- `GET /flow/{flowId}`：获取流程详情
- `POST /flow`：创建流程
- `POST /flow/design/{flowId}`：保存流程设计内容
- `PUT /flow/{flowId}`：更新流程基础信息
- `DELETE /flow/{flowId}`：删除流程
- `POST /flow/{flowId}/publish`：发布流程

注意：前端当前封装了 `unpublish` 和 `execute`，但 `FlowController` 中未看到对应接口，后续需要补齐或调整前端。

## 数据模型

`FlowDefinition` 主要字段：

- `id`
- `name`
- `description`
- `version`
- `status`：`draft`、`published`、`archived`
- `definitionJson`：流程定义 JSON，使用 `JacksonTypeHandler` 存为 `Map<String, Object>`
- `publishedAt`
- `createdBy`
- `createdAt`
- `updatedAt`

`definitionJson` 当前主要包含：

- `nodes`
- `edges`
- `config`

`FlowDefinitionDTO` 字段：

- `id`
- `name`
- `description`
- `nodes`
- `edges`
- `config`

`FlowNode` 字段：

- `id`
- `type`
- `name`
- `x`
- `y`
- `config`
- `inputs`
- `outputs`

## 发布流程

`FlowPublishService.publish` 的主要步骤：

1. 查询 `FlowDefinition`。
2. 将 `definitionJson` 转成 `FlowDefinitionDTO`。
3. 校验流程必须有节点、网关节点和 `response` 节点。
4. 调用 `FlowExecutor.publish`，将流程放入内存缓存。
5. 调用 `GatewayRegistrar.register` 注册动态网关。
6. 更新状态为 `published`，写入 `publishedAt`。

## 执行流程

`FlowExecutor` 的核心逻辑：

- 已发布流程缓存在内存 `flowCache`。
- 起始节点优先取没有输入连线的节点；没有连线时取第一个节点。
- 根据节点 `type` 从 `executorMap` 找 `ComponentExecutor`。
- 节点执行结果会写入：
  - `lastResult`
  - `{nodeId}_output`
  - 节点 `outputs` 声明的变量
- 连线可配置 `condition`。
- 条件表达式使用 JavaScript 脚本引擎求值，变量包括 `context`、`input`、`output`、`lastResult`。
- 多个下游节点当前是循环执行后汇总结果，虽然代码注释称并行，但目前不是线程级并行。

## 当前后端执行器类型

当前已看到的 `ComponentExecutor.getType()`：

- `agent-skill`
- `api-gateway`
- `function-call`
- `mcp-tool`
- `rest-api`
- `stream-api`
- `websocket-api`
- `http`

前端还有很多节点类型暂未看到后端执行器，例如：

- `database`
- `redis`
- `transform`
- `filter`
- `condition`
- `java`
- `python`
- `javascript`
- `response`
- `ai-chat`

如果发布或执行包含这些节点，可能会遇到“不支持的组件类型”。

## 动态网关

`GatewayRegistrar` 根据网关节点 `config.protocol` 注册入口：

- `rest`：`RestApiRegistrar`
- `stream`：`StreamApiRegistrar`
- `websocket`：`WebSocketRegistrar`
- `mcp`：`McpServer`
- `function`：注册到 `/api/functions/{functionName}`
- `skill`：注册到 `/api/skills/{skillName}`

网关节点类型包括：

- `api-gateway`
- `rest-api`
- `stream-api`
- `websocket-api`
- `mcp-tool`
- `function-call`
- `agent-skill`

## 执行器实现类映射

| getType() | 实现类 | 所在目录 |
|-----------|--------|----------|
| `api-gateway` | ApiGatewayExecutor | component/gateway |
| `rest-api` | RestApiExecutor | component/gateway |
| `stream-api` | StreamApiExecutor | component/gateway |
| `websocket-api` | WebSocketExecutor | component/gateway |
| `mcp-tool` | McpToolExecutor | component/gateway |
| `function-call` | FunctionCallExecutor | component/gateway |
| `agent-skill` | AgentSkillExecutor | component/gateway |
| `http` | HttpComponentExecutor | component/http |

## 已知问题汇总

### P0 — 阻碍性

1. **Controller 缺少 unpublish 和 execute 接口**：`FlowPublishService.unpublish()` 和 `FlowExecutor.execute()` 已实现，但 `FlowController` 未暴露对应端点。前端已封装这两个接口，当前调用会 404。

2. **response 节点无执行器**：发布校验 `validateFlow` 强制要求流程包含 `response` 节点，但没有对应的 `ComponentExecutor` 实现。一旦流程执行到 response 节点会抛"不支持的组件类型"。

3. **designFlow 返回 null**：`FlowServiceImpl.designFlow()` 保存成功后返回 `null`，Controller 声明返回 `Result<FlowDefinition>`。如果前端依赖返回值（如获取更新后的流程数据），会出问题。

### P1 — 功能性

4. **10 个节点类型缺少执行器**：`database`、`redis`、`transform`、`filter`、`condition`、`java`、`python`、`javascript`、`response`、`ai-chat`。前端可拖拽这些节点，但后端执行时报"不支持的组件类型"。

5. **executeParallel 不是真正的并行**：方法名和注释称"并行执行"，实际是串行 for 循环依次执行多个下游节点。

6. **数据库无初始化脚本**：`pom.xml` 引入了 Flyway 依赖，但没有迁移文件（`src/main/resources/db/migration` 不存在），也没有建表 SQL 脚本。

### P2 — 优化性

7. **deleteFlow 真删除**：当前直接 `removeById`，代码注释提到后续改成逻辑删除（状态改为 archived）。

8. **JavaScript 引擎兼容性**：条件连线依赖 `ScriptEngineManager.getEngineByName("javascript")`，Java 17 默认不包含 Nashorn，需要确认运行环境是否有 GraalJS 或额外配置。

## 开发注意事项

- 新增节点执行能力时，实现 `ComponentExecutor` 并注册为 Spring Bean，`FlowExecutor.init()` 会自动放入 `executorMap`。
- 新增前端节点类型时，要同步确认后端执行器类型、发布校验、动态网关注册逻辑。
- 当前端接口报 404 时，优先检查前端 `src/api/flow.js` 和后端 `FlowController` 是否对齐。
