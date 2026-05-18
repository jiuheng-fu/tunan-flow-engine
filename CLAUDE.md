# CLAUDE.md

## 项目概述

`tunan-flow-engine` 是拖拽式接口生成平台的流程引擎后端，基于 Spring Boot 3.5.4 + Java 17 + Maven。管理流程定义、发布、动态网关注册、流程执行。为前端 `flow-designer` 提供 REST API。

## 常用命令

```sh
mvn spring-boot:run    # 启动服务（端口 3000）
mvn test               # 运行测试
```

## 技术栈

- Java 17, Spring Boot 3.5.4, Maven
- Spring MVC + WebFlux + WebSocket
- PostgreSQL + MyBatis-Plus 3.5.5 + JacksonTypeHandler
- Redis（已引入依赖）、Caffeine 本地缓存
- Lombok, Hutool, Guava
- JavaScript 脚本引擎（条件连线求值）

## 目录结构

```
src/main/java/com/tunan/flow/
├── TunanFlowEngineApplication.java    # 启动入口
├── config/                            # FlowEngineConfig, WebSocketConfig
├── common/                            # Result, ResultCode, LoginBody
├── entity/FlowDefinition.java         # 表 flow_definition
├── dto/                               # FlowNode, FlowEdge, FlowDefinitionDTO, InterfaceConfig, ApiParam, ExecutionResult
├── mapper/FlowDefinitionMapper.java   # MyBatis-Plus Mapper
├── controller/
│   ├── FlowController.java            # /flow CRUD + publish
│   └── McpController.java             # MCP 接口
├── service/
│   ├── FlowService.java               # 接口
│   ├── FlowPublishService.java        # publish / unpublish / republish
│   └── impl/FlowServiceImpl.java      # CRUD 实现
├── engine/
│   ├── FlowExecutor.java              # 流程执行核心（发布/执行/条件判断）
│   ├── ExecutionContext.java          # 执行上下文
│   ├── component/
│   │   ├── ComponentExecutor.java     # 节点执行器接口（getType + execute）
│   │   ├── http/HttpComponentExecutor.java
│   │   └── gateway/                   # ApiGateway, RestApi, StreamApi, WebSocket, McpTool, FunctionCall, AgentSkill
│   └── gateway/
│       ├── GatewayRegistrar.java      # 统一网关注册分发
│       ├── RestApiRegistrar.java      # REST 动态路由注册
│       ├── StreamApiRegistrar.java    # SSE/Stream
│       ├── WebSocketRegistrar.java    # WebSocket 注册
│       ├── McpServer.java             # MCP 服务
│       └── Flow*Handler.java          # 各类请求处理器
└── exception/                         # GlobalException, GlobalExceptionHandler
```

## 数据模型

### flow_definition 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String (ASSIGN_ID) | 主键 |
| name | String | 流程名称 |
| description | String | 描述 |
| version | Integer | 版本 |
| status | String | draft / published / archived |
| definition_json | Map (JacksonTypeHandler) | 流程定义 JSON，含 nodes/edges/config |
| published_at | LocalDateTime | 发布时间 |
| created_by | String | 创建人 |
| created_at | LocalDateTime | 自动填充 |
| updated_at | LocalDateTime | 自动填充 |

### FlowNode 字段
id / type / name / x / y / config(Map) / inputs(List<String>) / outputs(List<String>)

## 已有的节点执行器（9个）

| getType() | 类 |
|-----------|-----|
| `api-gateway` | ApiGatewayExecutor |
| `rest-api` | RestApiExecutor |
| `stream-api` | StreamApiExecutor |
| `websocket-api` | WebSocketExecutor |
| `mcp-tool` | McpToolExecutor |
| `function-call` | FunctionCallExecutor |
| `agent-skill` | AgentSkillExecutor |
| `http` | HttpComponentExecutor |

## 目前不支持但前端存在的节点类型（执行时会抛"不支持的组件类型"）

`response`, `database`, `redis`, `transform`, `filter`, `condition`, `java`, `python`, `javascript`, `ai-chat`

## 动态网关

GatewayRegistrar 根据网关节点 `config.protocol` 注册：

| protocol | 注册器 | 说明 |
|----------|--------|------|
| `rest` | RestApiRegistrar | 动态注册 REST 端点 |
| `stream` | StreamApiRegistrar | SSE/流式 |
| `websocket` | WebSocketRegistrar | 支持 server/client 模式 |
| `mcp` | McpServer | MCP 工具注册 |
| `function` | → `/api/functions/{functionName}` | 复用 REST |
| `skill` | → `/api/skills/{skillName}` | 复用 REST |

## 已知问题

1. **Controller 缺接口**：`unpublish` 和 `execute` 前端已封装，但 FlowController 未暴露。FlowPublishService 和 FlowExecutor 已有对应方法，只需在 Controller 加端点。
2. **designFlow 返回 null**：`FlowServiceImpl.designFlow()` 返回 null，Controller 声明返回 `Result<FlowDefinition>`，前后端不一致。
3. **executeParallel 名不副实**：方法是串行 for 循环，不是真正的线程级并行。
4. **deleteFlow 真删除**：注释说后续改逻辑删除。
5. **响应节点无执行器**：发布校验要求必须有 `response` 节点，但没有对应的 ComponentExecutor。
6. **数据库无初始化脚本**：pom.xml 有 flyway 依赖但无迁移文件，没有建表 SQL。
7. **JavaScript 引擎可用性**：条件连线依赖 Nashorn/GraalJS，Java 17 默认环境需确认。

## 开发约定

- 新增节点执行器：实现 `ComponentExecutor`，标注 `@Component`，`FlowExecutor.init()` 通过 `@Autowired List<ComponentExecutor>` 自动收集。
- 前后端节点类型必须对齐，否则发布校验或执行会报错。
- 前端通过 `/api` 代理访问后端，自动去掉 `/api` 前缀。
- Controller 使用统一响应包装 `Result<T>`。
- 配置文件：`src/main/resources/application.yml`，数据库连接 `localhost:5433/flow_ai_engine`。
