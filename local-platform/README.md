# local-platform

Starts every backend service as its own jar — one command instead of four IDE run configs.

```bash
docker compose up -d
mvn -pl local-platform exec:java
```

| Flag | Example |
|---|---|
| Skip a service | `-Dalpha.local.skip=payment` |
| Force rebuild | `-Dalpha.local.repackage=true` |
| Repo path | `-Dalpha.local.backend=/path/to/repo` |

Services: config-server `:8888`, account `:8081`, screening `:8082`, payment `:8083`.
