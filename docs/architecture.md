# Architecture

## Goal

TraceLens has one job: produce a useful latency breakdown for one Spring HTTP request without requiring external infrastructure.

## Request lifecycle

1. A servlet `Filter` or reactive `WebFilter` creates a `TraceContext`.
2. Integrations append bounded `TraceSpan` records to that context.
3. On request completion TraceLens computes **application self time** as total request duration minus the union of all captured span intervals.
4. A `TraceReport` is published to all `TraceReporter` beans.
5. The built-in reporter prints the tree-like breakdown.
6. `Server-Timing` is emitted when the HTTP response lifecycle still allows it.

The interval-union calculation matters because nested/overlapping spans must not be double-subtracted.

## Context propagation

Servlet requests use a `ThreadLocal` because JDBC, `RestTemplate`, `RestClient` and imperative Redis execute on the request thread in the normal case.

WebFlux uses Reactor Context. `WebClient` reads the request `TraceContext` from Reactor Context and writes the completed outbound span directly to that captured context, so completion may happen on a different thread.

TraceLens does not promise transparent context propagation through arbitrary user-created executors. Use a custom span around the async boundary or a full tracing system when that is required.

## JDBC

Instrumentation occurs at `DataSource#getConnection()` and uses JDK interface proxies for `Connection`, `Statement`, `PreparedStatement` and `CallableStatement`.

This captures the time spent executing JDBC calls and works below Hibernate/Spring Data JPA, so ORM users do not need a Hibernate-specific plugin.

Result-set iteration time is intentionally not represented as SQL execution time. It remains part of application self time.

## HTTP clients

- `RestTemplate`: `ClientHttpRequestInterceptor`
- `RestClient`: `RestClientCustomizer`
- `WebClient`: `ExchangeFilterFunction` through `WebClientCustomizer`

Only host is logged by default. Query strings are never included.

## Redis

Imperative Spring Data Redis is instrumented at the `RedisConnectionFactory` boundary. Returned command groups are proxied and timed without reading command arguments, keys or values.

Reactive Redis is not yet instrumented because timing a method that merely returns a `Publisher` would be incorrect; it requires subscription-aware instrumentation.

## Safety and failure isolation

TraceLens must never become an availability dependency:

- integration code is no-op when no request is being traced;
- span count is bounded;
- reporters are exception-isolated;
- final/concrete infrastructure beans are left untouched when safe subclass proxying cannot preserve bean type;
- no request body, response body, SQL bind value or Redis key/value is captured.
