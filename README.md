# Task Tally Backend

Minimal Quarkus starter for Task‑tally. Provides REST APIs for user preferences, credential references and GitHub template operations.

## Dev Environment

To set up and run the development environment, follow these steps:

### 1. Start a Postgres container using Podman

```sh
podman run --name task-tally-postgres -e POSTGRES_USER=tasktally -e POSTGRES_PASSWORD=tasktally -e POSTGRES_DB=tasktally -p 5432:5432 -d postgres:15
```

This command will start a Postgres 15 container with the default credentials and database name used by the application.

### 2. Start the development environment

```sh
./mvnw compile quarkus:dev
```

This will launch the Quarkus development server. The application will connect to the Postgres instance started above.

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

## Using SSH with GitHub/GitLab

1. **Generate key**
```bash
ssh-keygen -t ed25519 -C "tasktally-bot" -f id_ed25519
```

2. **Register public key**
- GitHub → Repo → Settings → Deploy Keys → Add deploy key → Allow write access
- GitLab → Project → Settings → Repository → Deploy Keys → enable write

3. **Create Kubernetes Secret**
```bash
kubectl create secret generic tasktally-git-user123 \
     --from-file=id_ed25519=./id_ed25519 \
     --from-file=id_ed25519.pub=./id_ed25519.pub \
     --from-file=known_hosts=./known_hosts
```

4. **known_hosts example**
```bash
ssh-keyscan -t ed25519 github.com >> known_hosts
ssh-keyscan -t ed25519 gitlab.com >> known_hosts
```

5. **Link credential**
```bash
curl -X POST $BASE/api/preferences/me/credentials   -H "Content-Type: application/json" -H "X-User-Id: user123"   -d '{
      "name":"my-gh-deploy-key",
      "provider":"github",
      "scope":"write",
      "secretRef":"k8s:secret/tasktally-git-user123#id_ed25519",
      "knownHostsRef":"k8s:secret/tasktally-git-user123#known_hosts"
    }'
```

6. **Validate SSH**
```bash
curl -X POST $BASE/api/git/ssh/validate   -H "Content-Type: application/json" -H "X-User-Id: user123"   -d '{"provider":"github","owner":"acme","repo":"templates","branch":"main","credentialName":"my-gh-deploy-key"}'
```

7. **Push templates/proposals**
Use SSH remote: `git@github.com:owner/repo.git`.

8. **Codegen instructions**
- Always read `AGENTS.md` before generating code.
- Use prompt snippets in §10 for entities, REST, SecretResolver, and tests.
- Never output raw secrets; use placeholders + SecretRefs.
- New DB schema changes must ship with Flyway migrations + tests.

## Health
Standard Quarkus health endpoints are exposed at `/q/health`.
