# tracking-logging

Shared Spring Boot library for **enterprise workflow tracing + logging** across CodestoryKH microservices.

Designed for banking flows (payments, fund transfers, onboarding) where observability must never break a business transaction.

---

## What this library does

### 1. Zero financial impact (fail-safe)

Telemetry bugs must never abort core business work.

- `WorkflowTracker.recordTelemetry(...)` is wrapped in `FailSafe`
- Metric/log failures are swallowed and warned on `TRACKING_FAILSAFE` / tracker logger
- Business exceptions are still rethrown after step recording

### 2. Distributed correlation (MDC)

Create the tracker **after** setting MDC at the request/transaction boundary. The tracker snapshots MDC (`transactionId`, `channel`, `traceId`, …) and restores it when async steps complete.

### 3. Sync + async workflow steps

- `runStep` — synchronous step with Micrometer timer + structured log
- `runStepAsync` — wraps a `CompletableFuture`, records status on completion (any thread)
- Step results are stored in a `ConcurrentLinkedQueue` (safe under concurrent completions)
- `getSummary()` returns overall status, total duration, and per-step results

Also included:

- `TraceContext` — fail-safe `traceId` / `spanId` / `recordHttpFailure`
- Shared Logback fragment with OTLP appender
- `OpenTelemetryAppenderInitializer` auto-config

---

## Main components

| Component | Package | Role |
|-----------|---------|------|
| `WorkflowTracker` | `workflow` | Per-transaction step tracker |
| `WorkflowTrackerFactory` | `workflow` | Spring bean to create trackers |
| `WorkflowTrackerProperties` | `config` | `management.workflow-tracker.*` |
| `FailSafe` | `support` | Never-throw wrapper |
| `TraceContext` | `util` | OTEL span helpers |
| `OpenTelemetryAppenderInitializer` | `config` | Wire Logback → OTLP |
| `InstantFundTransferService` | `demo` | Reference usage pattern |
| `otel-logging.xml` | resources | Shared Logback include |

---

## How a consuming service uses this library

### Step 1 — Maven dependency

```xml
<dependency>
    <groupId>com.codestorykh</groupId>
    <artifactId>tracking-logging</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Step 2 — Logback

`src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="com/codestorykh/tracking/logging/logback/otel-logging.xml"/>
</configuration>
```

### Step 3 — Configuration

```yaml
management:
  workflow-tracker:
    enabled: true
    metric-prefix: banking.payment.step.duration
    enable-percentiles: true
  tracing:
    sampling:
      probability: 1.0
  opentelemetry:
    resource-attributes:
      "[service.name]": ${spring.application.name}
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
```

### Step 4 — Business code

```java
@Service
public class FundTransferService {

    private final WorkflowTrackerFactory trackerFactory;

    public FundTransferService(WorkflowTrackerFactory trackerFactory) {
        this.trackerFactory = trackerFactory;
    }

    public TransferResponse transfer(TransferRequest request) {
        MDC.put("transactionId", request.transactionId());
        MDC.put("channel", "MOBILE_APP");

        WorkflowTracker tracker = trackerFactory.create("INSTANT_FUND_TRANSFER", request.transactionId());

        try {
            tracker.runStep("VALIDATE", () -> {
                validate(request);
                return null;
            });

            CompletableFuture<Boolean> aml = tracker.runStepAsync(
                    "AML_SCREENING",
                    () -> amlClient.screenAsync(request)
            );
            if (!Boolean.TRUE.equals(aml.join())) {
                throw new SecurityException("AML rejected");
            }

            tracker.runStep("CORE_DEBIT", () -> ledger.debit(request));
            tracker.runStep("CORE_CREDIT", () -> ledger.credit(request));

            return TransferResponse.ok(request.transactionId());
        } finally {
            log.info("Workflow summary: {}", tracker.getSummary());
            MDC.clear();
        }
    }
}
```

See also `demo/InstantFundTransferService` for a full pipeline example.

---

## Auto-configured beans

| Bean | Condition |
|------|-----------|
| `OpenTelemetryAppenderInitializer` | `OpenTelemetry` present |
| `WorkflowTrackerFactory` | `MeterRegistry` present + `management.workflow-tracker.enabled=true` (default) |

---

## Logging levels and sensitive data

Treat **log level** and **sensitive data** as two separate controls.

### Log level

| Level | Use for |
|-------|---------|
| `ERROR` | Failures that need action |
| `WARN` | Recoverable / business rejects |
| `INFO` | Step boundaries + correlation ids |
| `DEBUG` | Detailed flow (dev/staging only) |
| `TRACE` | Raw payloads (almost never in prod) |

```yaml
logging:
  level:
    root: INFO
    com.codestorykh: INFO
```

### Sensitive data

Log level does **not** protect secrets. Never log (any level): PAN, CVV, PIN, OTP, passwords, tokens, raw KYC blobs.

**Safe:** `traceId`, `spanId`, `channel`, `transactionId`, masked account (`****8494`), step name + duration + error code.

```java
MDC.put("sourceAccount", maskAccount(account)); // ****8494 — not full account
```

**Summary:** `INFO` for steps + correlation in prod; mask or omit sensitive fields. Level controls volume; masking prevents leaks.

---

## Design rules

1. Set MDC **before** `trackerFactory.create(...)` so the snapshot includes correlation ids.
2. Let business exceptions propagate from `runStep` / async futures — tracker records then rethrows / marks FAILED.
3. Always `MDC.clear()` in `finally` on request threads.
4. Do not put raw PII into MDC or Prometheus tags (`error_code` is class simple name only).
5. Treat tracking as best-effort — business success must not depend on telemetry side effects.

---

## Module coordinates

- **Group:** `com.codestorykh`
- **Artifact:** `tracking-logging`
- **Package root:** `com.codestorykh.tracking.logging`
