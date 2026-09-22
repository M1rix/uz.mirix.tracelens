# TraceLens

**Know why your Spring request is slow. One dependency. Zero infrastructure.**

TraceLens is a lightweight request profiler for Spring Boot. It gives you an immediate per-request breakdown of time spent in application code, SQL/JPA, outbound HTTP, Redis and response serialization/write time — without Grafana, Jaeger, Zipkin, an OpenTelemetry Collector, agents or a sidecar.

```text
TraceLens GET /api/orders/{id}  200  684.12 ms  [trace=41e5bc4f28ce45d1]
├─ application self time                         55.14 ms
├─ SQL SELECT: select ...                       143.02 ms
├─ SQL SELECT: select ...                        58.10 ms
├─ HTTP GET inventory-service                   391.06 ms
├─ Redis GET                                      8.22 ms
└─ serialization / response write                17.11 ms
```

TraceLens also emits a `Server-Timing` response header so the same breakdown can be inspected in browser DevTools.

## What it is / what it is not

TraceLens is deliberately **not** an APM platform. It is a small developer dependency for local development, performance debugging, integration environments and small services where installing observability infrastructure would be overkill.

Its contract is simple:

- one dependency;
- zero or near-zero setup;
- no external infrastructure;
- immediate visible result;
- removable without changing application architecture.

## Install

The project is currently at `0.1.0-SNAPSHOT`. Once published, usage is:

```xml
<dependency>
    <groupId>uz.mirix.tracelens</groupId>
    <artifactId>tracelens-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

For local development of the starter:

```bash
mvn clean install
```

Then add the dependency to a Spring Boot 3.5.x application. The starter depends on `spring-web`, but **does not pull Spring MVC into a WebFlux application**.

## Zero-config integrations

| Area | Supported | How |
|---|---|---|
| Spring MVC / servlet requests | ✅ | servlet filter |
| Servlet async request lifetime | ✅ | `AsyncListener` completion |
| Spring WebFlux requests | ✅ | reactive web filter |
| JDBC / Hibernate / Spring Data JPA | ✅ | `DataSource` connection/statement interception |
| `RestTemplate` | ✅ | interceptor + bean post processor |
| `RestClient` | ✅ | Boot `RestClientCustomizer` |
| `WebClient` | ✅ | Boot `WebClientCustomizer`, Reactor-context aware |
| Spring Data Redis (imperative) | ✅ | connection factory / command interception |
| R2DBC | ❌ | planned; JDBC instrumentation does not see R2DBC |
| Reactive Redis | ❌ | planned |
| arbitrary raw Java `HttpClient` | ❌ | no global JVM agent is installed |

The unsupported cases are intentional: TraceLens does not use bytecode instrumentation or a Java agent.

> Servlet async request **duration** is tracked to completion. Work executed on arbitrary application-managed executors does not inherit the TraceLens `ThreadLocal` automatically; use a custom span around that boundary or a full distributed tracing solution.

## Configuration

Defaults are designed to show value immediately:

```yaml
tracelens:
  enabled: true
  sample-rate: 1.0
  max-spans: 100
  slow-threshold: 250ms

  logging:
    enabled: true
    mode: ALL # ALL | SLOW_ONLY
    include-trace-id: true

  server-timing:
    enabled: true
    max-metrics: 20

  web:
    exclude:
      - /actuator/**
      - /favicon.ico
      - /error

  sql:
    enabled: true
    max-length: 180

  http-client:
    enabled: true
    include-path: false

  redis:
    enabled: true
```

For production-like traffic, do not blindly profile everything:

```yaml
tracelens:
  sample-rate: 0.05
  logging:
    mode: SLOW_ONLY
  slow-threshold: 500ms
```

## Custom spans

Use the static API only when TraceLens cannot infer a useful boundary itself:

```java
try (var ignored = TraceLens.span("pricing-rules")) {
    return pricingEngine.calculate(order);
}
```

or:

```java
var result = TraceLens.trace("fraud-check", () -> fraudClient.check(order));
```

When there is no active traced request these calls are no-ops.

## Security by default

TraceLens is designed for debugging without turning logs into a secret dump:

- prepared statement parameter values are never captured;
- SQL string literals, numeric literals, UUIDs and comments are redacted before logging;
- outbound HTTP query strings are never logged;
- outbound paths are disabled by default;
- Redis keys and values are never logged;
- exception messages are not included, only exception types;
- labels are single-line and bounded in length.

This is defense in depth, not a guarantee that every database dialect or custom statement form can be perfectly anonymized. Treat profiling logs with the same access controls as application logs.

## `Server-Timing`

HTTP headers must be known before the response commits, so `Server-Timing` is a **pre-commit view** of the trace. TraceLens injects it immediately before the first servlet body write or from WebFlux `beforeCommit`. The final log report is produced after request completion and therefore contains the complete request duration.

Responses with no body receive the header at normal filter completion when the response is still mutable.

## Extending TraceLens

Add a Spring bean implementing `TraceReporter`:

```java
@Bean
TraceReporter myReporter() {
    return report -> {
        // send to an internal debug sink, write a test assertion, etc.
    };
}
```

A reporter must be fast and thread-safe. Reporter failures are isolated and never fail the application request.

## Design boundaries

TraceLens intentionally does **not**:

- replace OpenTelemetry;
- propagate distributed trace context between services;
- store historical traces;
- expose a dashboard;
- collect SQL bind parameters;
- collect request/response bodies;
- inject an agent or bytecode transformer.

If you need cross-service tracing and long-term retention, use OpenTelemetry/APM. TraceLens is for answering one local question quickly: **where did this request spend its time?**

## Build

```bash
mvn -B -ntp verify
```

CI verifies Java 17, 21 and 25.

## License

Apache License 2.0.
