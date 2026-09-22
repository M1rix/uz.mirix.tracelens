# Configuration reference

All properties are under `tracelens`.

| Property | Default | Meaning |
|---|---:|---|
| `enabled` | `true` | Master switch; disabled means instrumentation beans are not created |
| `sample-rate` | `1.0` | Request sampling ratio (`0..1`) |
| `max-spans` | `100` | Per-request span budget |
| `slow-threshold` | `250ms` | WARN threshold |
| `logging.enabled` | `true` | Built-in log reporter |
| `logging.mode` | `ALL` | `ALL` or `SLOW_ONLY` |
| `logging.include-trace-id` | `true` | Include local trace id in logs |
| `server-timing.enabled` | `true` | Add `Server-Timing` |
| `server-timing.max-metrics` | `20` | Header metric budget |
| `server-timing.include-descriptions` | `false` | Expose span descriptions in browser header |
| `server-timing.max-description-length` | `80` | Max description length when enabled |
| `web.exclude` | actuator/favicon/error | Ant-style request exclusions |
| `sql.enabled` | `true` | JDBC instrumentation |
| `sql.max-length` | `180` | Max sanitized SQL label |
| `http-client.enabled` | `true` | Spring HTTP client instrumentation |
| `http-client.include-path` | `false` | Include sanitized path (never query string) |
| `redis.enabled` | `true` | Imperative Spring Data Redis instrumentation |

Detailed `Server-Timing` descriptions can contain database schema names, internal service hostnames or custom span names. They are therefore disabled by default. Enable them only where exposing that metadata to the HTTP client is acceptable:

```yaml
tracelens:
  server-timing:
    include-descriptions: true
```

## Recommended profiles

### Local development

Use defaults, optionally enabling `server-timing.include-descriptions` when detailed DevTools labels are useful.

### Shared test environment

```yaml
tracelens:
  sample-rate: 0.25
  logging:
    mode: SLOW_ONLY
  slow-threshold: 300ms
```

### Production incident window

Enable temporarily and sample aggressively enough to answer a specific latency question:

```yaml
tracelens:
  enabled: true
  sample-rate: 0.01
  logging:
    mode: SLOW_ONLY
  slow-threshold: 750ms
```

TraceLens is not a historical APM. For continuous production observability use OpenTelemetry/APM and use TraceLens as a focused developer tool.
