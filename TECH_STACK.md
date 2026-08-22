# TECH_STACK.md

# AI Story Co-Author v0.1.1

## Principle

> **Java owns business state. Python owns AI computation.**

## Frontend

Vue 3 + TypeScript + Vite + Vue Router + Pinia + Axios。

## Backend

当前仓库真实使用：

```text
Java 17
Spring Boot 3.5.x
Spring Web / Validation
MyBatis 3 + XML Mapper
MySQL 8.4
Maven
```

早期文档曾目标 Java 21，但 v0.1.1 不为了文档一致单独升级 Java。

## AI Service

Python 3.12+、FastAPI、Pydantic、LangChain、langchain-openai compatible client、单一配置 LLM Provider。

## Communication

`Vue → Spring Boot → Python`，HTTP + JSON。

## Persistence / Migration

MySQL + MyBatis。继续使用仓库现有 SQL migration 方式；v0.1.1 不主动引入 Flyway/Liquibase。迁移 additive first。

## Background Generation

允许 Spring TaskExecutor / @Async / ExecutorService。禁止为了 Continuous 引入 MQ/Redis。

## Tests

Backend：JUnit 5 / Spring Boot Test / MockMvc / Mockito。Python：pytest / FastAPI TestClient。Frontend：`npm run build`。真实 AI 行为通过真实 LLM acceptance run 验证。

## Security

Secrets 只通过环境变量，不提交 API Key/PAT。日志可记录 model、prompt version、run id 和非敏感 context，但不得打印 Secret。

## Explicit Exclusions

Redis、Kafka、RabbitMQ、RocketMQ、Spring Cloud、Nacos、Consul、Elasticsearch、Vector DB、Neo4j、GraphRAG、LangGraph、Spring AI、JPA/Hibernate、MyBatis-Plus、Kubernetes。

## Future Trigger

只有 Structured Memory 与近期 Context 修好后仍无法找回久远正文具体细节，才评估 RAG；只有单机 executor 无法满足真实可靠性/吞吐，才评估 MQ。
