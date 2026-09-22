# Contributing

Keep TraceLens lightweight.

A proposed feature should satisfy most of these conditions:

1. it helps explain one request's latency;
2. it works without external infrastructure;
3. it is zero/near-zero configuration;
4. it does not collect sensitive payload data;
5. it does not require an agent;
6. it has bounded memory/CPU overhead;
7. it can be disabled or removed without architecture changes.

Before submitting changes:

```bash
mvn -B -ntp verify
```

Add tests for timing math, redaction, or integration boundaries when behavior changes.
