# Architecture

## Goal

TraceLens has one job: produce a useful latency breakdown for one Spring HTTP request without requiring external infrastructure.

## Request lifecycle

1. A servlet `Filter` or reactive `WebFilter` creates a `TraceContext`.
2. Integrations append bounded `TraceSpan` records to that context.
3. On request completion TraceLens computes **application self time** as total request duration minus the union of all captured span intervals.
4. A `TraceReport` is published to all `TraceReporter` beans.
5. The built-in reporter prints the tree-like breakdown.
6. `Server-Timing` is emitted before HTTP response commitment.

The interval-union calculation matters because nested, parallel and overlapping spans must not be double-subtracted from application self time. Individual spans may overlap, so their displayed durations are not expected to sum arithmetically to the total request duration.

## Context propagation

Servlet requests use a `ThreadLocal` because JDBC, `RestTemplate`, `RestClient` and imperative Redis normally execute on the request thread.

WebFlux uses Reactor Context. `WebClient` reads the request `TraceContext` from Reactor Context and writes the completed outbound span directly to that captured context, so completion may happen on a different thread.

Application-defined reactive spans use `ReactiveTraceLens.trace(...)`, which reads Reactor Context at subscription time. The imperative `TraceLens.span(...)` API intentionally does not pretend that a `ThreadLocal` can follow reactive thread switches.

TraceLens does not promise transparent context propagation through arbitrary user-created executors. A full tracing system is the right tool when that is required.

## Servlet async requests

For servlet async processing, TraceLens registers an `AsyncListener` and keeps the request trace open until servlet completion. This makes the reported request lifetime correct even when the initial filter invocation returns early.

Arbitrary work submitted by the application to its own executor does not automatically inherit the TraceLens `ThreadLocal`.

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

## Dependency boundary

The starter has a hard dependency on `spring-web`, but WebFlux and Redis are optional integrations. It does not pull `spring-webmvc` into a reactive application and therefore does not change Spring Boot's web application type merely by being installed.

## Safety and failure isolation

TraceLens must never become an availability dependency:

- integration code is a no-op when no request is being traced;
- span count is bounded;
- reporters are exception-isolated;
- final/concrete infrastructure beans are left untouched when safe subclass proxying cannot preserve bean type;
- no request body, response body, SQL bind value or Redis key/value is captured.
