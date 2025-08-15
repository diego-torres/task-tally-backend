# Task Tally Backend

Minimal Quarkus starter for Task‑tally. Provides REST APIs for user preferences, credential references and GitHub template operations.

## Runtime configuration
All configuration is supplied via environment variables.

| Variable | Description |
|---|---|
| `DB_JDBC_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `GITHUB_API_BASE` | Base URL for GitHub API (default `https://api.github.com`) |
| `K8S_SECRET_BASE_PATH` | Base path for mounted Kubernetes secrets |

## Example secret reference
`k8s:secret/mysecret#token` → reads file `${K8S_SECRET_BASE_PATH}/mysecret/token` or env `MYSECRET_TOKEN`.

## Example YAML template
```yaml
name: demo
description: Example template
activities:
  - role: dev
    task: code
    estimateHours: 4
```

## API examples
Assuming the application runs on `localhost:8080`.

```bash
# Get preferences
curl -H 'X-User-Id: u1' http://localhost:8080/api/preferences/me

# Upsert preferences
curl -X PUT -H 'X-User-Id: u1' -H 'Content-Type: application/json' \
  -d '{"ui":{"theme":"dark"}}' http://localhost:8080/api/preferences/me

# Link GitHub credential
curl -X POST -H 'X-User-Id: u1' -H 'Content-Type: application/json' \
  -d '{"name":"gh","ref":"k8s:secret/mysecret#token"}' http://localhost:8080/api/github/link

# Pull templates
curl -X POST -H 'X-User-Id: u1' -H 'Content-Type: application/json' \
  -d '{"owner":"o","repo":"r","path":"templates","branch":"main","credentialName":"gh"}' \
  http://localhost:8080/api/github/templates/pull
```

## Health
Standard Quarkus health endpoints are exposed at `/q/health`.
