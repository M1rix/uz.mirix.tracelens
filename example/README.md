# TraceLens Example

This application is intentionally a consumer of TraceLens, not another module of the library.

GET /demo/orders/42 performs a real H2 query through JdbcTemplate, then an outbound HTTP call through Boot's customized RestClient.Builder, then normal Spring MVC JSON serialization.

The outbound request targets GET /inventory/42 in the same demo process, so no Docker, mock server, or external infrastructure is required.

## Run

```bash
mvn -B -ntp install
mvn -B -ntp -f example/pom.xml spring-boot:run
```

Then call:

```bash
curl -i http://localhost:8080/demo/orders/42
```

The response contains Server-Timing metrics. Detailed descriptions are enabled because this is a local demo.

## Smoke test

```bash
mvn -B -ntp -f example/pom.xml verify
```

The smoke test boots the app on a random port, performs a real request, and asserts that SQL and outbound HTTP spans appear in Server-Timing.
