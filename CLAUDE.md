# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

拖拽式接口生成平台的流程引擎后端。Spring Boot 3.5.4 + Java 17 + Maven，端口 3000，为前端 `flow-designer` 提供 REST API。

## 常用命令

```sh
mvn spring-boot:run                          # 启动
mvn test                                     # 全部测试
mvn test -Dtest=FlowExecutorTest             # 单个测试类
mvn test -Dtest=FlowExecutorTest#testExecute # 单个测试方法
mvn compile                                  # 仅编译
```

## 核心架构

### 三层执行模型

```
Controller  →  FlowPublishService  →  FlowExecutor (内存) + GatewayRegistrar (动态路由)
                    ↓                         ↓
            FlowServiceImpl             ComponentExecutor
           (数据库 CRUD)               (节点类型 → 执行逻辑)
```

**FlowExecutor** 通过 `@Autowired List<ComponentExecutor>` + `@PostConstruct init()` 自动发现所有执行器 Bean，按 `getType()` 建立 `executorMap`。`FlowEngineConfig` 显式扫描 `com.tunan.flow.engine.component` 和 `com.tunan.flow.engine.gateway` 两个包。

**GatewayRegistrar** 读取网关节点 `config.protocol`，分发到具体注册器（RestApiRegistrar 等），创建动态路由。网关执行器（如 RestApiExecutor）只透传配置到上下文变量 `_gatewayConfig` / `_protocol`，实际请求处理由对应的 Handler 完成。

### 执行流程

1. 找起始节点：优先取无输入连线的节点，无连线时取第一个
2. `executorMap.get(node.type)` 获取执行器，找不到抛 "不支持的组件类型"
3. 执行结果写入上下文：`lastResult`、`{nodeId}_output`、节点 `outputs` 列表
4. 查找下游节点时对每条连线做 `condition` 条件求值（JavaScript `ScriptEngine`）
5. 多下游节点串行执行（`executeParallel` 名不副实，实际是 for 循环）

### 变量传递

- `HttpComponentExecutor` 支持 `{{variableName}}` 模板替换 URL/headers/body 中的变量
- `ExecutionContext.setLastResult()` 同时写入 `lastResult`、`_lastResult`、`variables["lastResult"]`、`variables["_lastResult"]`
- 节点输入准备：收集所有入边源节点的 `{sourceId}_output`，再合并全局 `variables`

## 关键约定

- 新增执行器：实现 `ComponentExecutor` + `@Component`，`getType()` 返回值要与前端节点 `type` 一致
- `definitionJson` 通过 MyBatis-Plus `JacksonTypeHandler` 存为 `Map<String, Object>`，内部结构 `{nodes, edges, config}`
- Controller 统一用 `Result<T>` 包装响应
- 前端通过 `/api` 代理，后端路径不含 `/api` 前缀
- 数据库：`localhost:5433/flow_ai_engine`，用户 `postgres`，密码 `postgres123`

## 当前可用的执行器

| getType() | 类 | 职责 |
|-----------|-----|------|
| `http` | HttpComponentExecutor | HTTP 调用，支持 `{{var}}` 模板替换 |
| `rest-api` | RestApiExecutor | REST 入口，透传配置到上下文 |
| `stream-api` | StreamApiExecutor | 流式入口 |
| `websocket-api` | WebSocketExecutor | WebSocket 入口 |
| `api-gateway` | ApiGatewayExecutor | 通用 API 网关入口 |
| `mcp-tool` | McpToolExecutor | MCP 工具入口 |
| `function-call` | FunctionCallExecutor | OpenAI Function Call 入口 |
| `agent-skill` | AgentSkillExecutor | Agent Skill 入口 |

## 已知问题

1. **Controller 缺 unpublish 和 execute 端点** — Service 层已实现，只需加接口
2. **response 节点无执行器** — 发布校验强制要求，但执行时会抛异常
3. **designFlow 返回 null** — Controller 声明 `Result<FlowDefinition>`，与实现不一致
4. **executeParallel 实际串行** — 多下游节点时 for 循环而非线程并行
5. **数据库无初始化脚本** — pom.xml 有 Flyway 但无迁移文件
6. **JavaScript 引擎兼容性** — Java 17 默认不含 Nashorn，条件连线依赖 `ScriptEngineManager`
