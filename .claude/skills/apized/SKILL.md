---
name: apized
description: This skill should be used when the user asks to build an API, add a model, add an endpoint, create a new entity, implement a behavior, configure security, add federation, write a repository extension, or work with any part of the apized framework. Activates on questions like "how do I add a new model", "create a REST endpoint", "add a behavior", "configure permissions", or "use @Apized".
metadata:
  version: 1.0.0
  last_synced_commit: 317df29cc68a8e7277b1f5b14b4a910e9ca2cd09
---

# Apized Framework Guide

Apized is an annotation-driven JVM framework that auto-generates REST API infrastructure (controller, service, repository, deserializer) from a single annotated model class.

## Quick Reference

| Section | What it covers |
|---|---|
| [Gradle Setup](#gradle-setup) | Plugin + optional module dependencies |
| [Model Definition](#model-definition) | `@Apized`, `BaseModel`, scope hierarchy, `layers`, `operations` |
| [Service Extensions](#service-extensions) | Shared logic between behaviours |
| [Repository Extensions](#repository-extensions) | Custom queries, dynamic finders, `@Query` |
| [Behaviours](#behaviours) | Lifecycle hooks, ordering, touched fields, original state |
| [Context Access](#context-access) | `ApizedContext` — request, security, audit, events |
| [REST Query Features](#rest-query-features) | `?fields=`, `?search=`, pagination |
| [Security & Permissions](#security--permissions) | Permission format, UserResolver, `@Owner`, enrichers, filters |
| [Federation](#federation) | Cross-API references with `@Federation` |
| [Audit & Events](#audit--events) | Automatic trails, custom events, RabbitMQ config |
| [Controller Extensions](#controller-extensions) | Override generated actions |
| [Behaviour Pipeline on Custom Endpoints](#triggering-the-behaviour-pipeline-from-custom-endpoints) | `@MicronautBehaviourExecution` / `@SpringBehaviourExecution` |
| [Custom Controllers](#custom-controllers) | Fully custom endpoints |
| [Tracing](#tracing-module) | `@Traced` + OpenTelemetry |
| [Distributed Lock](#distributed-lock-module) | `LockFactory` + ShedLock |
| [Testing](#test-module) | Cucumber BDD integration tests |

## Core Concept

Annotate a model with `@Apized` → annotation processor generates all layers at compile time.

## Gradle Setup (consumer project)

**Micronaut:**
```gradle
plugins {
  id 'org.apized.micronaut' version "$apizedVersion"
}

dependencies {
  annotationProcessor 'io.micronaut.openapi:micronaut-openapi'              // optional: OpenAPI docs
  implementation "org.apized:micronaut-tracing:$apizedVersion"              // optional: OTEL tracing
  implementation "org.apized:micronaut-messaging-rabbitmq:$apizedVersion"   // optional: RabbitMQ events
  implementation "org.apized:micronaut-distributed-lock:$apizedVersion"     // optional: distributed locks
}
```

The Micronaut plugin reads `apized.properties` at the project root to select the SQL dialect and automatically add the matching JDBC driver and Flyway dependency:

```properties
# apized.properties
dialect=POSTGRES   # H2 | MYSQL | POSTGRES | SQL_SERVER | ORACLE | ANSI (default)
```

**Spring:**
```gradle
plugins {
  id 'org.apized.spring' version "$apizedVersion"
}

dependencies {
  implementation "org.apized:spring-tracing:$apizedVersion"              // optional: OTEL tracing
  implementation "org.apized:spring-messaging-rabbitmq:$apizedVersion"   // optional: RabbitMQ events
  implementation "org.apized:spring-distributed-lock:$apizedVersion"     // optional: distributed locks
}
```

The apized Gradle plugin handles annotation processor wiring and code generation configuration automatically.

## Model Definition

```java
@Entity
@Getter @Setter
@Apized(
  layers = {Layer.CONTROLLER, Layer.SERVICE, Layer.REPOSITORY},
  operations = {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE},
  audit = true,
  event = true,
  maxPageSize = 50,
  extensions = MyRepositoryExtension.class
)
public class Product extends BaseModel {
  @NotBlank
  private String name;

  private BigDecimal price;

  @ManyToOne
  private Category category;

  @OneToMany(mappedBy = "product", orphanRemoval = true)
  private List<Review> reviews;
}
```

**Rules:**
- Extend `BaseModel` (provides `id` UUID PK, `version` Long for optimistic locking — if the client sends it the update will fail if stale (useful for sensitive operations like decrementing a stock counter); if omitted the framework applies a request-scoped optimistic lock, `createdBy`, `createdAt`, `lastUpdatedBy`, `lastUpdatedAt`, `metadata` JSON-like Map/List for unstructured data)
- Annotate with `@Entity` (JPA) and `@Apized`
- Use Lombok `@Getter @Setter`
- `layers` controls which classes are generated. Default: all three (`CONTROLLER`, `SERVICE`, `REPOSITORY`). Omit a layer to suppress it — e.g. `layers = {Layer.SERVICE, Layer.REPOSITORY}` generates no HTTP endpoints, useful for internal-only models.
- `operations` controls which CRUD endpoints are exposed. Default: all five (`LIST`, `GET`, `CREATE`, `UPDATE`, `DELETE`).
- Request bodies for `CREATE` and `UPDATE` are automatically validated with Bean Validation (`@Valid`). Annotate model fields with `@NotBlank`, `@NotNull`, `@Size`, etc. as needed.
- `@ManyToMany` relationships are managed automatically — the framework generates `add/remove` methods for the join table and calls them when the relationship field is included in a PUT request. No custom code needed.
- `extensions` accepts an array — pass multiple extension classes: `extensions = {RepoExtension.class, ServiceExtension.class}`.
- Use `scope` in `@Apized` to define the parent model and establish a hierarchy (used for endpoint URL nesting and integration test tooling):

```java
@Entity
@Apized(scope = Organization.class)
public class Department extends BaseModel { ... }
```

## Service Extensions

Add custom methods to the generated service, or override standard CRUD actions. The extension must be a concrete bean — the generated service injects it and delegates calls to it.

```java
@Singleton   // @Component for Spring
@Apized.Extension(layer = Layer.SERVICE)
public class ProductServiceExtension {
  public BigDecimal calculateDiscountedPrice(Product product, double discountPct) {
    return product.getPrice().multiply(BigDecimal.valueOf(1 - discountPct));
  }
}
```

Reference it via `@Apized(extensions = ProductServiceExtension.class)`. The generated `ProductService` will inject this bean and expose `calculateDiscountedPrice` as a public method, available to all behaviours that inject `ProductService`.

Use `@Apized.Extension.Action` to replace a standard CRUD action entirely:

```java
@Singleton   // @Component for Spring
@Apized.Extension(layer = Layer.SERVICE)
public class ProductServiceExtension {
  @Inject ProductRepository repository;

  @Apized.Extension.Action(Action.LIST)
  public Page<Product> list(int page, int pageSize, List<SearchTerm> search, List<SortTerm> sort) {
    // custom list logic — replaces the generated list method
    return repository.findActiveProducts(page, pageSize);
  }
}
```

## Repository Extensions

Add custom queries by creating an interface and referencing it in `@Apized(extensions = ...)`:

```java
@Apized.Extension(layer = Layer.REPOSITORY)
public interface ProductRepositoryExtension {
  List<Product> findByCategory(UUID categoryId);

  Page<Product> findByPriceGreaterThan(BigDecimal price, Pageable pageable);
}
```

Use `exclude` to suppress generated methods you don't want (works on any layer):
```java
@Apized.Extension(layer = Layer.REPOSITORY, exclude = {"someGeneratedMethod"})
```

## Behaviours

Implement custom logic before/after any CRUD operation. Behaviours must be registered as beans — `@Singleton` in Micronaut, `@Component` in Spring:

```java
@Singleton   // @Component for Spring
@Behaviour(
  model = Product.class,
  layer = Layer.SERVICE,
  when = {When.BEFORE, When.AFTER},
  actions = {Action.CREATE, Action.UPDATE},
  order = 100   // lower runs first
)
public class ProductValidationBehaviour implements BehaviourHandler<Product> {

  // Inject dependencies normally — @Inject (Micronaut) or @Autowired (Spring)
  @Inject CategoryRepository categoryRepository;

  @Override
  public void preCreate(Execution<Product> execution, Product input) {
    // runs BEFORE create
  }

  @Override
  public void postCreate(Execution<Product> execution, Product output) {
    // runs AFTER create
  }

  // also: preList, postList, preGet, postGet, preUpdate, postUpdate, preDelete, postDelete
}
```

`Execution<T>` contains `id` (UUID), `input` (T), and `output` (T).

### Touched fields and original state

`_getModelMetadata()` exposes two key helpers inside behaviours:

```java
@Override
public void preUpdate(Execution<Order> execution, Order input) {
  // Check which fields were actually sent in the request
  if (input._getModelMetadata().getTouched().contains("password")) {
    input.setPassword(BCrypt.hashpw(input.getPassword(), BCrypt.gensalt()));
  }

  // Force a computed field to be persisted even if client didn't send it
  input.setVerificationCode(generateCode());
  input._getModelMetadata().getTouched().add("verificationCode");

  // Access the pre-update snapshot for delta detection
  Order original = (Order) input._getModelMetadata().getOriginal();
  if (!input.getStatus().equals(original.getStatus())) {
    // status changed — react to the transition
  }
}
```

### Behaviour execution order

The `order` parameter controls execution sequence across all behaviours for the same model+action. **Lower values run first; negative values run before positive ones:**

```java
@Behaviour(model = Order.class, when = When.BEFORE, actions = Action.CREATE, order = -1000)
public class GenerateReference implements BehaviourHandler<Order> { ... }  // runs 1st

@Behaviour(model = Order.class, when = When.BEFORE, actions = Action.CREATE, order = -999)
public class EnrichFares implements BehaviourHandler<Order> { ... }  // runs 2nd

@Behaviour(model = Order.class, when = When.BEFORE, actions = Action.CREATE, order = 100)
public class ProcessPayment implements BehaviourHandler<Order> { ... }  // runs last
```

## Key Annotations Reference

| Annotation | Purpose |
|---|---|
| `@Apized` | Mark model for code generation |
| `@Apized.Extension` | Add custom methods to a generated layer |
| `@Behaviour` | Register a lifecycle hook |
| `@Federation` | Reference entity from an external API |
| `@PermissionEnricher` | Custom permission evaluation logic |
| `@AuditField` / `@AuditIgnore` | Control audit field tracking |
| `@EventField` / `@EventIgnore` | Control event field payload |
| `@Owner` | Mark ownership for permission checks |

## Key Enums

```java
Layer   → CONTROLLER, SERVICE, REPOSITORY
Action  → LIST, GET, CREATE, UPDATE, DELETE, NO_OP
When    → BEFORE, AFTER
```

## Context Access

Use `ApizedContext` (static, thread-local) inside behaviors or services:

```java
ApizedContext.getRequest()    // headers, path variables, timestamp
ApizedContext.getSecurity()   // current user, token, permissions
ApizedContext.getAudit()      // current audit entries
ApizedContext.getEvent()      // current events
ApizedContext.getFederation() // federation cache and config
ApizedContext.getSerde()      // internal framework use only — do not use in application code
```

## REST Query Features

All generated endpoints support these query parameters:

| Feature | Syntax | Example |
|---|---|---|
| Field filtering | `?fields=f1,f2` | `?fields=name` |
| Model drilling | `?fields=f1,rel.f2` | `?fields=name,employees.name` |
| Partial PUT | `?fields=f1` + partial body | send only changed fields |
| Search | `?search=field<op>value` | `?search=name=Org%20A` |
| Nested search | `?search=rel.field<op>value` | `?search=employee.name~=Sen` |
| Pagination | `?page=1&pageSize=50` | page is 1-based; default pageSize is 50, capped by `maxPageSize` on `@Apized` |
| Sorting | `?sort=field>,other<` | `>` = ASC, `<` = DESC; comma-separated; default ASC if no suffix |

Search operators for `?search=`: `=` (eq), `!=` (ne), `~=` (like/contains), `>` (gt), `>=` (gte), `<` (lt), `<=` (lte), `<>` (in — e.g. `status<>ACTIVE,PENDING`), `<!>` (nin — e.g. `status<!>DRAFT,CANCELLED`). Multiple search terms are comma-separated.

Model drilling works on both GET and PUT. LIST responses return a `Page<T>`:

```json
{
  "content": [...],
  "page": 1,
  "pageSize": 50,
  "totalPages": 5,
  "total": 98
}
```

## Search & Sorting (programmatic)

```java
List<SearchTerm> search = List.of(
  SearchTerm.builder().field("name").op(SearchOperation.like).value("phone").build(),
  SearchTerm.builder().field("price").op(SearchOperation.gte).value(10.0).build()
);
List<SortTerm> sort = List.of(
  SortTerm.builder().field("name").direction(SortDirection.ASC).build()
);
Page<Product> results = service.list(1, 50, search, sort);
```

Search operators: `eq`, `ne`, `like`, `gt`, `gte`, `lt`, `lte`, `in`, `nin`

## Security & Permissions

Permission format: `{slug}.{entity}.{action}[.{id}][.{field}][.{value}]` — wildcards supported.

| Permission | Meaning |
|---|---|
| `*` | God permission — anything on any service |
| `sample` | Everything in the sample service |
| `sample.organization` | All operations on all organizations |
| `sample.organization.create` | Can create organizations |
| `sample.organization.update.{id}` | Can update specific organization |
| `sample.organization.update.*.name` | Can update name of any organization |
| `sample.address.update.*.country.PT` | Can update any address country to PT only |

Permissions are evaluated for **every affected model and field** in a request (including nested models via model drilling).

**Configuration:**
```yaml
apized:
  slug: myapp          # REQUIRED — used as the first segment of all permission strings
  cookie: token        # cookie name for token auth (default: token)
  token: ${APP_TOKEN}  # this service's own identity token for inter-service calls (federation, RabbitMQ, ESB); required when security is enabled
  exclusions:          # paths that bypass security/filter processing
    - /health.*
    - /swagger.*
```

**Authentication:** `Authorization: Bearer <token>` header, or cookie (name set by `apized.cookie`).

**UserResolver:** Implement `UserResolver` to replace the default `MemoryUserResolver`. It builds `User` + `Role` objects for each request. The `User` object accepts a `metadata` map for carrying extra data (e.g. external system IDs) through the request lifecycle.

> **Default (dev only):** `MemoryUserResolver` is active when no custom `UserResolver` is registered. It ignores the token and always returns a hardcoded admin user with `permissions: ["*"]`. Replace it in production.

**Micronaut:**
```java
@Singleton
@Replaces(MemoryUserResolver.class)
public class DBUserResolver implements UserResolver {
  @Override
  public User getUser(String token) {
    return User.builder()
      .id(userId)
      .name("Alice Smith")                          // full display name
      .username("user@example.com")
      .permissions(permissions)                     // direct permission strings
      .roles(roles)                                 // optional; isAllowed() checks roles too
      .metadata(Map.of("stripeCustomerId", stripeId))
      .build();
  }
}
```

**Spring:**
```java
@Component
@Primary
public class DBUserResolver implements UserResolver {
  @Override
  public User getUser(String token) {
    return User.builder()
      .id(userId)
      .name("Alice Smith")                          // full display name
      .username("user@example.com")
      .permissions(permissions)                     // direct permission strings
      .roles(roles)                                 // optional; isAllowed() checks roles too
      .metadata(Map.of("stripeCustomerId", stripeId))
      .build();
  }
}
```

You can also execute code as a specific user via `userResolver.runAs()`:

```java
userResolver.runAs(targetUser, () -> {
  // all ApizedContext.getSecurity() calls inside here see targetUser
  orderService.create(order);
});
```

```java
// Manual permission check inside a behaviour/service:
ApizedContext.getSecurity().getUser().isAllowed("myapp.product.create");
```

**Inferred (runtime) permissions** via three mechanisms:

### 1. @Owner
```java
@Owner(actions = { Action.GET }, permissions = @Permission(action = Action.UPDATE, fields = "owner"))
private UUID owner;
```

The `fields` value in `@Permission` supports a `"field.VALUE"` syntax to make permissions conditional on a field's current value:

```java
// Owner can update the order, but only the listed fields, and status only when set to COMPLETE
@Owner(
  actions = { Action.LIST, Action.GET, Action.CREATE },
  permissions = @Permission(
    action = Action.UPDATE,
    fields = { "status.COMPLETE", "orderItems", "customer", "paymentMethod", "metadata" }
  )
)
private UUID owner;
```

### 2. PermissionEnricher (model-scoped)

**Micronaut:**
```java
@Singleton
@PermissionEnricher(Booking.class)
public class BookingPermissionEnricher implements PermissionEnricherHandler<Booking> {
  @Inject BookingRepository bookingRepository;
  @Inject ApizedConfig config;

  @Override
  public boolean enrich(Class<Model> type, Action action, Execution<Booking> execution) {
    User user = ApizedContext.getSecurity().getUser();
    Booking booking = bookingRepository.get(execution.getId()).orElseThrow();
    if (booking.getOwner().equals(user.getId())) {
      user.getInferredPermissions().add(config.getSlug() + ".booking.get." + booking.getId());
      return true;
    }
    return false;
  }
}
```

**Spring:**
```java
@Component
@PermissionEnricher(Booking.class)
public class BookingPermissionEnricher implements PermissionEnricherHandler<Booking> {
  @Autowired BookingRepository bookingRepository;
  @Autowired ApizedConfig config;

  @Override
  public boolean enrich(Class<Model> type, Action action, Execution<Booking> execution) {
    User user = ApizedContext.getSecurity().getUser();
    Booking booking = bookingRepository.get(execution.getId()).orElseThrow();
    if (booking.getOwner().equals(user.getId())) {
      user.getInferredPermissions().add(config.getSlug() + ".booking.get." + booking.getId());
      return true;
    }
    return false;
  }
}
```

### 3. Server Filter (global)

**Micronaut:**
```java
@ServerFilter(Filter.MATCH_ALL_PATTERN)
class MyPermissionFilter extends ApizedServerFilter {
  @RequestFilter
  @ExecuteOn(TaskExecutors.BLOCKING)
  void filterRequest(HttpRequest<?> request) {
    if (shouldExclude(request.getServletPath())) return;
    User user = ApizedContext.getSecurity().getUser();
    // add to user.getInferredPermissions() based on path variables etc.
  }
  @Override public int getOrder() { return ServerFilterPhase.SECURITY.after(); }
}
```

**Spring:**
```java
@Component
class MyPermissionFilter extends ApizedServerFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!shouldExclude(request.getServletPath())) {
      User user = ApizedContext.getSecurity().getUser();
      // add to user.getInferredPermissions() based on path variables etc.
    }
    chain.doFilter(request, response);
  }
  // Built-in order: HttpRequestFilter=1, SecurityFilter=2, SerdeFilter=3 — run after security
  @Override public int getOrder() { return 4; }
}
```

## Federation (cross-API references)

Every apized server instance acts as an API gateway. You can reference models that live on a remote service using `@Federation` — the framework will transparently fetch them when requested via model drilling.

**Important:** The root object of any query must be requested from the server that owns that model directly. Federation only resolves nested references.

### Declaring a federated field

```java
@Entity
@Getter @Setter
@Apized
public class Order extends BaseModel {
  // 'catalog' is the federation alias; Item is the remote type
  // uri template resolves {catalogItemId} from the field of that name on this entity
  @Federation(value = "catalog", type = "Item", uri = "/items/{catalogItemId}")
  private Item catalogItem;

  private UUID catalogItemId;
}
```

When a client requests `?fields=catalogItem.name`, apized calls the remote catalog service to resolve `Item` and inlines the result.

### Configuration

```yaml
# application.yml
apized:
  federation:
    catalog:                               # alias used in @Federation(value = ...)
      baseUrl: https://catalog-api.example.com
      headers:                             # optional; if Authorization absent, falls back to global apized.token
        Authorization: "Bearer ${CATALOG_TOKEN}"
      queryParams:                         # optional extra query params appended to every federation call
        - "version=2"
```

### FederationContext

The `FederationContext` maintains a per-request cache of already-fetched federated models to avoid duplicate remote calls:

```java
ApizedContext.getFederation()  // access cache and federation config inside behaviours
```

## Audit & Events

- **Audit**: Every CREATE/UPDATE/DELETE is recorded automatically when `audit = true`. Pass `X-Reason` header to attach a reason.
- **Events**: Published on CREATE/UPDATE/DELETE when `event = true`. Format: `{slug}.{entityType}.{action}d` (e.g., `myapp.product.created`).

Event delivery requires a messaging adapter. For RabbitMQ, add `micronaut-messaging-rabbitmq` (or `spring-messaging-rabbitmq`) and configure:

```yaml
rabbitmq:
  exchange: apized   # topic exchange name (default: "apized"); routing key = event type
  # standard rabbitmq connection properties (uri, host, port, etc.)
```

`ApizedStartupEvent` is fired by the framework after initialization. Use it to bootstrap data (e.g. ensure default roles/users exist):

```java
// Micronaut
@EventListener
void onStartup(ApizedStartupEvent event) { ensureDefaults(); }

// Spring
@EventListener
void onStartup(ApizedStartupEvent event) { ensureDefaults(); }
```

You can inject custom headers into all outgoing event messages for the current request:

```java
// adds to AMQP message headers alongside timestamp, token, and path variables
ApizedContext.getEvent().getHeaders().put("tenantId", tenantId.toString());
```

You can also publish custom events manually from a behaviour — they are queued in the request scope and delivered only if the request succeeds:

```java
ApizedContext.getEvent().add(new Event(
  ApizedContext.getRequest().getId(),  // correlationId
  "notification.email.order-complete", // event type / routing key
  Map.of("orderId", order.getId(), "userId", order.getOwner())  // payload
));
```

Use `@AuditIgnore` / `@EventIgnore` to exclude a field from the snapshot, and `@AuditField` / `@EventField` to include a computed/virtual value (can be placed on a method):

```java
public class Order extends BaseModel {
  private BigDecimal unitPrice;
  private Integer quantity;

  @AuditIgnore   // never appears in audit snapshots
  @EventIgnore   // never appears in event payloads
  private String internalNote;

  @AuditField    // included in audit snapshot even though it's not a persisted column
  @EventField
  public BigDecimal getTotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
```

For relationships, use `@EventField({"id", "name"})` to restrict which sub-fields appear in the event payload (avoids large nested payloads):

```java
@EventField({"id", "name"})   // only id and name of each role are included in events
@ManyToMany
private List<Role> roles;
```

## Generated Classes

For a model `Product`, the processor generates:
- `ProductRepository` (implements `ModelRepository<Product>`)
- `ProductService` (implements `ModelService<Product>`) — also exposes `batchCreate`, `batchUpdate`, `batchDelete` for bulk operations (service-layer only, not exposed as HTTP endpoints)
- `ProductController` (implements `ModelController<Product>`)
- `ProductDeserializer`

All are annotated with `@Generated` — do not edit them directly. Use extensions and behaviors instead.

`ProductService` exposes two fetch methods with different semantics:
- `get(id)` — runs through the full behaviour pipeline (use from controllers or external callers)
- `find(id)` — bypasses behaviours (use inside behaviours to avoid triggering the pipeline recursively)
- `searchOne(List<SearchTerm> search)` — returns `Optional<T>` for finding a single record by field criteria instead of by UUID (e.g. find a user by email)

## Triggering the Behaviour Pipeline from Custom Endpoints

Use `@MicronautBehaviourExecution` (Micronaut) or `@SpringBehaviourExecution` (Spring) to run the behaviour pipeline for a given model+layer+action from a custom controller method:

**Micronaut:**
```java
@Controller("/search/trips")
public class SuggestionsController {
  @Inject TripService service;

  @Get
  @MicronautBehaviourExecution(execution =
    @BehaviourExecution(model = Trip.class, layer = Layer.CONTROLLER, action = Action.LIST)
  )
  public Page<Trip> suggest(@QueryValue String location) {
    List<SearchTerm> search = List.of(
      SearchTerm.builder().field("origin").op(SearchOperation.like).value(location).build()
    );
    return service.list(1, 10, search, List.of());
  }
}
```

**Spring:**
```java
@RestController
@RequestMapping("/search/trips")
public class SuggestionsController {
  @Autowired TripService service;

  @GetMapping
  @SpringBehaviourExecution(execution =
    @BehaviourExecution(model = Trip.class, layer = Layer.CONTROLLER, action = Action.LIST)
  )
  public Page<Trip> suggest(@RequestParam String location) {
    List<SearchTerm> search = List.of(
      SearchTerm.builder().field("origin").op(SearchOperation.like).value(location).build()
    );
    return service.list(1, 10, search, List.of());
  }
}
```

This ensures any behaviours registered for `Trip` / `CONTROLLER` / `LIST` fire around your custom method, just as they would for the generated endpoint.

## Controller Extensions

Use `@Apized.Extension(layer = Layer.CONTROLLER)` to override a generated action (e.g. replace hard-delete with soft-delete):

```java
@Apized.Extension(layer = Layer.CONTROLLER)
public class RouteControllerExtension {
  @Inject RouteService routeService;

  @Apized.Extension.Action(Action.DELETE)
  public Route delete(UUID id) {
    Route route = routeService.get(id);
    route.setDeleted(true);
    return routeService.update(id, route);
  }
}
```

Reference it via `@Apized(extensions = RouteControllerExtension.class)`. The framework calls your method instead of the generated DELETE.

## Custom Controllers

For operations that don't fit CRUD (e.g. login, password reset, token exchange), write a fully custom controller and inject the generated services.

**Micronaut:**
```java
@Controller("/users/{username}/password")
@ExecuteOn(TaskExecutors.BLOCKING)   // required for any blocking I/O (JPA, JDBC)
public class PasswordResetController {
  @Inject UserRepository userRepository;
  @Inject UserService userService;

  @Delete
  @Status(HttpStatus.ACCEPTED)
  public void requestReset(String username) {
    userRepository.findByUsername(username).ifPresent(user -> {
      user.setResetCode(CodeGenerator.generate());
      userService.update(user.getId(), user);
    });
  }

  @Post
  public void resetPassword(String username, @Body ResetRequest body) {
    User user = userRepository.findByUsername(username).orElseThrow(NotFoundException::new);
    if (!user.getResetCode().equals(body.code())) throw new UnauthorizedException();
    user.setPassword(body.newPassword());
    userService.update(user.getId(), user);
  }
}
```

**Spring:**
```java
@RestController
@RequestMapping("/users/{username}/password")
public class PasswordResetController {
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;

  @DeleteMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestReset(@PathVariable String username) {
    userRepository.findByUsername(username).ifPresent(user -> {
      user.setResetCode(CodeGenerator.generate());
      userService.update(user.getId(), user);
    });
  }

  @PostMapping
  public void resetPassword(@PathVariable String username, @RequestBody ResetRequest body) {
    User user = userRepository.findByUsername(username).orElseThrow(NotFoundException::new);
    if (!user.getResetCode().equals(body.code())) throw new UnauthorizedException();
    user.setPassword(body.newPassword());
    userService.update(user.getId(), user);
  }
}
```

Custom controllers have full access to `ApizedContext`, all generated services, and repository extensions.

## Complete Example

### Parent model (top-level)

```java
@Entity
@Getter @Setter
@Apized(
  operations = {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE},
  audit = true,
  event = true
)
public class Organization extends BaseModel {
  @NotBlank
  private String name;

  @OneToMany(mappedBy = "organization", orphanRemoval = true)
  private List<Department> departments;
}
```

### Child model (scoped under Organization)

```java
@Entity
@Getter @Setter
@Apized(
  scope = Organization.class,
  operations = {Action.LIST, Action.GET, Action.CREATE, Action.UPDATE, Action.DELETE}
)
public class Department extends BaseModel {
  @NotBlank
  private String name;

  @ManyToOne
  private Organization organization;

  @ManyToOne
  private Employee manager;
}
```

This generates endpoints like `GET /organizations/{organization}/departments/{id}`.

### Behaviour on the child

```java
@Singleton   // @Component for Spring
@Behaviour(
  model = Department.class,
  layer = Layer.SERVICE,
  when = When.BEFORE,
  actions = Action.CREATE
)
public class DepartmentCreationBehaviour implements BehaviourHandler<Department> {
  @Override
  public void preCreate(Execution<Department> execution, Department input) {
    // e.g. default the manager to the current user
    UUID currentUser = ApizedContext.getSecurity().getUser().getId();
    if (input.getManager() == null) {
      Employee mgr = new Employee();
      mgr.setId(currentUser);
      input.setManager(mgr);
    }
  }
}
```

### Custom repository query

```java
@Apized.Extension(layer = Layer.REPOSITORY)
public interface DepartmentRepositoryExtension {
  Page<Department> findByOrganizationAndManagerId(UUID organizationId, UUID managerId, Pageable pageable);
}
```

Reference it via `@Apized(extensions = DepartmentRepositoryExtension.class)` on the model.

---

## Tracing Module (`micronaut-tracing` / `spring-tracing`)

Apply `@Traced` to any method or class to create an OpenTelemetry span automatically:

```java
// Simple usage — span name defaults to "ClassName::methodName"
@Traced
public void processOrder(UUID orderId) { ... }

// Custom name + kind + attributes referencing method parameters
@Traced(
  value = "payment.charge",
  kind = TraceKind.CLIENT,
  attributes = {
    @Traced.Attribute(key = "order.id", arg = "orderId"),
    @Traced.Attribute(key = "gateway", value = "stripe")
  }
)
public void chargeCard(UUID orderId, BigDecimal amount) { ... }
```

`TraceKind` values: `INTERNAL` (default), `SERVER`, `CLIENT`, `PRODUCER`, `CONSUMER`.

Requires a `Tracer` bean in the context (e.g. via the OpenTelemetry SDK). If no `Tracer` bean is present the interceptor is inactive.

---

## Distributed Lock Module (`micronaut-distributed-lock` / `spring-distributed-lock`)

Inject `LockFactory` to run code under a named distributed lock (backed by ShedLock over JDBC). Use `@Inject` (Micronaut) or `@Autowired` (Spring).

```java
@Inject LockFactory lockFactory;  // or @Autowired for Spring

// Basic — acquire lock, run task
lockFactory.executeWithLock("generate-invoices", () -> {
  invoiceService.generateMonthly();
});

// With retry — keep trying to acquire lock for up to 30 seconds
lockFactory.executeWithLockRetry("send-notifications", Duration.ofSeconds(30), () -> {
  notificationService.sendPending();
});

// Full control — lock held at least 5s, at most 10min, with retry
lockFactory.executeWithLock("sync-catalog", true, Duration.ofSeconds(5), Duration.ofMinutes(10), () -> {
  catalogService.sync();
});
```

Requires a `shedlock` table in the database (created by your Flyway/Liquibase migration).

---

## Test Module (`micronaut-test` / `spring-test`)

The test module provides a Cucumber BDD integration testing framework that understands apized model hierarchies and automatically builds correct nested URLs.

### Setup

**Micronaut:**
```groovy
// build.gradle
dependencies {
  testImplementation "org.apized:micronaut-test:$apizedVersion"
}

@CucumberOptions(glue = ['org.apized', 'com.yourcompany'])
class IntegrationTests extends MicronautTestServer { }
```

```groovy
@Singleton
@Replaces(AbstractMicronautUserResolverMock)
class TestUserResolver extends AbstractMicronautUserResolverMock {
  @Override
  Map<String, User> getKnownUsers() {
    return [
      "admin": User.builder().permissions(["*"]).build(),
      "alice": User.builder().permissions(["myapp.product.list"]).build(),
    ]
  }
}
```

**Spring:**
```groovy
// build.gradle
dependencies {
  testImplementation "org.apized:spring-test:$apizedVersion"
}

@CucumberOptions(glue = ['org.apized', 'com.yourcompany'])
class IntegrationTests extends SpringBootTestServer { }
```

```groovy
@Component
@Primary
class TestUserResolver extends AbstractSpringUserResolverMock {
  @Override
  Map<String, User> getKnownUsers() {
    return [
      "admin": User.builder().permissions(["*"]).build(),
      "alice": User.builder().permissions(["myapp.product.list"]).build(),
    ]
  }
}
```

### Built-in Cucumber steps

```gherkin
# Auth
Given I login as admin

# Context — set known IDs for scoped models
Given the context is
  | organization | <orgId> |

# CRUD
Given I list the products
Given I list the products as myList
Given I create a product with
  | name  | Widget |
  | price | 9.99   |
Given I create a product as myProduct with
  | name  | Widget |
Given I get a product with id {id}
Given I update a product with id {id} with
  | name  | Updated Widget |
Given I delete a product with id {id}

# Field expansion (model drilling)
Given the responses are expanded to contain employees.name

# Assertions
Then the request succeeds
Then the request fails
Then the response contains 3 elements
Then the response contains
  | name  | Widget |
Then the response element 0 contains
  | name  | Widget |
Then the response contains element with
  | name  | Widget |
Then the response path "content[0].name" contains Widget
Then the response matches products/widget.json   # loads /payloads/products/widget.json
```

Values in DataTable cells support:
- `{lastId}` / `{myAlias.id}` — reference IDs from previous responses
- `/regex/` — regex match in assertions
- `true` / `false` — parsed as boolean
- Numeric strings — parsed as numbers
