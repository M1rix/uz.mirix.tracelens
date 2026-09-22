# Changelog

## 0.1.0-SNAPSHOT

Initial implementation.

- Spring MVC and WebFlux request profiling
- application self-time calculation with overlap-safe interval union
- JDBC/Hibernate/JPA timing at `DataSource` level
- `RestTemplate`, `RestClient`, and `WebClient` timing
- imperative Spring Data Redis timing
- custom spans through `TraceLens`
- safe SQL sanitization
- bounded per-request span collection and sampling
- `Server-Timing` response header
- pluggable `TraceReporter`
- Spring Boot configuration metadata
- Java 17/21 CI
