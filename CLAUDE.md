# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What is Apized

Apized is a JVM framework that auto-generates rich REST APIs from annotated model classes. It supports two runtime engines — **Micronaut** (default) and **Spring Boot** — with a shared `core` module containing all engine-agnostic abstractions.

## Build Commands

```bash
# Initial setup: publish core to local Maven before building dependents
./gradlew -c first-settings.gradle publishToMavenLocal

# Run all tests with coverage report
./gradlew clean testCodeCoverageReport --info

# Run tests for a specific module
./gradlew :spring:spring-core:test
./gradlew :micronaut:micronaut-core:test

# Publish to GitHub Packages (CI only)
./gradlew clean publish

# Publish Gradle plugins (CI only, on version tags)
./gradlew clean publishPlugins
```

**Note:** Gradle daemon is disabled (`org.gradle.daemon=false`) due to annotation processor requirements.

## Project Layout

```
core/                        # Engine-agnostic abstractions (Model, Service, Repository, Controller, Contexts, Behaviours)
distributed-lock/            # Distributed lock abstraction
test/                        # Shared test utilities (Cucumber, Spock helpers)
tracing/                     # OpenTelemetry @Traced abstraction
spring/
  spring-core/               # Spring Boot 3.4 implementation
  spring-distributed-lock/
  spring-test/
  spring-tracing/
  spring-mcp/
  spring-gradle-plugin/
micronaut/
  micronaut-core/            # Micronaut 4 implementation
  micronaut-distributed-lock/
  micronaut-test/
  micronaut-tracing/
  micronaut-messaging-rabbitmq/
  micronaut-mcp/
  micronaut-gradle-plugin/
```

## REST Query Capabilities

All generated endpoints support these query parameters out of the box:

- **Field filtering** — `?fields=name` returns only the requested fields
- **Model drilling** — `?fields=name,employees.name` fetches related data in a single request (works on GET and PUT)
- **Partial updates** — PUT with `?fields=name` sends only the changed fields
- **Searching** — `?search=name=Org%20A` (exact) or `?search=employee.name~=Sen` (contains); supports nested model fields
- **Sorting** — `?sort=name>,price<` — append `>` for ASC, `<` for DESC; comma-separated for multiple fields (default ASC if no suffix)

## Architecture

### Core Abstractions (`core/src/main/java/org/apized/core/`)

All domain entities extend `BaseModel`, which provides: `id` (UUID, PK), `version` (Long, optimistic locking), `createdBy`, `createdAt`, `lastUpdatedBy`, `lastUpdatedAt`, and `metadata` (JSON-like Map/List structure for unstructured data). The MVC stack is:

- `AbstractModelController<T>` → `AbstractModelService<T>` → `AbstractModelRepository<T>`
- Each layer exposes pre/post hooks via the **Behaviour system**

### Behaviour System

`@Behaviour`-annotated beans are registered in `BehaviourManager` and fire around every lifecycle operation (`BEFORE`/`AFTER` × `CREATE`/`GET`/`LIST`/`UPDATE`/`DELETE`). Built-in behaviours include `AuditBehaviour` and `EventBehaviour`. Custom logic is injected by implementing `BehaviourHandler` and annotating with `@Behaviour`.

### Context System

`ApizedContext` provides static access to thread-local context objects:
- `RequestContext` — HTTP path variables, request metadata
- `SecurityContext` — current user, roles, permissions
- `SerdeContext` — serialization configuration
- `FederationContext` — per-request cache for federated (cross-API) model resolution
- `AuditContext` — change tracking state
- `EventContext` — queued domain events

Each framework (Spring/Micronaut) provides a `ContextProvider` implementation.

### ExecutionPlan

`ExecutionPlan` / `ExecutionStep` orchestrate cascading JPA operations (create, update, delete, add/remove many-to-many). Steps are built via a fluent API and executed in order, with proper JPA lifecycle hooks (`@PrePersist`, `@PostUpdate`, etc.) fired at the right moments.

### Search & Sort

`SearchTerm` (with operations: EQUALS, LIKE, IN, …) and `SortTerm` (ASC/DESC) are passed from controllers down to repositories. `SearchHelper` builds JPA/Micronaut Data criteria from these.

### Security

Permission format: `[project].[model].[action].[id].[field].[value]` (wildcards supported). Authorization is permission-based; roles are just groupings of permissions.

**Authentication:** Bearer token via `Authorization: Bearer <token>` header, or cookie (name configurable via `apized.cookie`, defaults to `token`).

**Service identity token (`apized.token`):** The application's own credential for inter-service communication — used as the fallback `Authorization: Bearer` in federation HTTP calls (overridden per-federation via `apized.federation.<alias>.headers.Authorization`), used to resolve the system user in RabbitMQ consumers, and included in ESB message headers. Only required when security is enabled.

**UserResolver:** Must implement `UserResolver` and register it to replace the default `MemoryUserResolver`. It assembles `User` + `Role` objects for the caller. For manual checks: `ApizedContext.getSecurity().getUser().isAllowed(...)`.

**Inferred (runtime) permissions** — three mechanisms:
1. `@Owner` on a field — grants permissions to the owner at runtime
2. `@PermissionEnricher(Model.class)` implementing `PermissionEnricherHandler<T>` — model-scoped inference
3. Server filter extending `ApizedServerFilter` — global inference (runs for every request)

### Tracing

`@Traced` annotation creates OpenTelemetry spans. `TraceKind` values: `INTERNAL` (default), `SERVER`, `CLIENT`, `PRODUCER`, `CONSUMER`.

## Key Technology Versions

- Java 21
- Spring Boot 3.4.2 (spring modules)
- Micronaut 4.10.6 (micronaut modules)
- Spock + JUnit Jupiter for tests; REST Assured + Testcontainers (MySQL, PostgreSQL, Oracle XE, MSSQL) for integration tests — **Docker is required** to run integration tests
- JaCoCo code coverage enforced at ≥ 50%

## Updating the skill

When updating the skill, ensure that the metadata.last_synced_commit is updated to reflect the last commit that has affected it. Check what other changes have happened since the current commit in the file and the new commit hash you're adding.