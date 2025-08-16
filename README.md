# Task Tally Backend


Minimal Quarkus starter for Task‑tally. Provides REST APIs for user preferences, credential references and Git template operations over SSH.

## Quickstart: Local Development Environment

### 1. Start All Services with Docker Compose

This project provides a `docker-compose.yml` to start Postgres, Vault, and other dependencies for local development.

```sh
docker-compose up -d
```

This will start all required containers. You can inspect logs with:
```sh
docker-compose logs -f
```

### 2. Bootstrap Vault (Local Only)

The `init-vault.sh` script will automatically run in the `vault-init` service (see `docker-compose.yml`). It enables KV v2, writes a `tasktally` policy, and issues a dev token saved at `vault/bootstrap/dev-app-token.txt`.

After containers are up, retrieve the token from `vault/bootstrap/dev-app-token.txt` and set it in your `src/main/resources/application.properties`:

```
quarkus.vault.url=http://localhost:8200
quarkus.vault.authentication=token
quarkus.vault.token=<paste token here>
```

### 3. Start the Backend (Quarkus)

You can run the backend in dev mode:
```sh
./mvnw compile quarkus:dev
```
The application will connect to the Postgres and Vault instances started above.

---

## Runtime configuration
All configuration is supplied via environment variables.

| Variable | Description |
|---|---|
| `DB_JDBC_URL` | JDBC URL for PostgreSQL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
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

# Link SSH credential (optional; defaults to `~/.ssh/id_rsa`)
curl -X POST -H 'X-User-Id: u1' -H 'Content-Type: application/json' \
  -d '{"name":"ssh","ref":"k8s:secret/mysecret#id_ed25519","scope":"write"}' http://localhost:8080/api/git/link

# Pull templates via SSH
curl -X POST -H 'X-User-Id: u1' -H 'Content-Type: application/json' \
  -d '{"repoUri":"git@github.com:o/r.git","path":"templates","branch":"main"}' \
  http://localhost:8080/api/git/templates/pull
```

## Using SSH Keys

Task‑tally can upload or generate SSH keys and keep the sensitive bytes in a
secret store. Postgres stores only references.

1. **Upload an existing key**
   ```bash
   curl -X POST $BASE/api/preferences/me/ssh-keys \
     -H "Content-Type: application/json" -H "X-User-Id: user123" \
     -d '{"name":"my-gh-key","provider":"github",\
          "privateKeyPem":"-----BEGIN OPENSSH PRIVATE KEY-----\\n...\\n-----END OPENSSH PRIVATE KEY-----\\n",\
          "knownHosts":"github.com ssh-ed25519 AAAA...\\n"}'
   ```
2. **Generate a new key server side**
   ```bash
   curl -X POST $BASE/api/preferences/me/ssh-keys/generate \
     -H "Content-Type: application/json" -H "X-User-Id: user123" \
     -d '{"name":"gen-key","provider":"github"}'
   ```
3. **Register the public key** with GitHub/GitLab as a deploy key (allow write).
4. **Provide known_hosts**
   ```bash
   ssh-keyscan -t ed25519 github.com >> known_hosts
   ```
5. **Validate and delete**
   ```bash
   curl -X POST $BASE/api/git/ssh/validate -H "Content-Type: application/json" \
     -H "X-User-Id: user123" -d '{"provider":"github","owner":"acme","repo":"templates","branch":"main","credentialName":"my-gh-key"}'

   curl -X DELETE $BASE/api/preferences/me/ssh-keys/my-gh-key -H "X-User-Id: user123"
   ```
The backend loads private key material into memory only when performing Git
operations and never stores raw secrets in Postgres.

## Using SSH with GitHub/GitLab

The application looks for a key at `~/.ssh/id_rsa` (or `id_ed25519`) and `~/.ssh/known_hosts` by default. You can also link a credential reference as shown above.

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
