# Task Tally Backend


Minimal Quarkus starter for Task‑tally. Provides REST APIs for user preferences, credential references and Git template operations over SSH.
## Quickstart: Local Development Environment

### 1. Start All Services with Docker Compose

This project provides a `docker-compose.yml` to start Postgres, Vault, and other dependencies for local development.

```sh
podman compose up -d
```

This will start all required containers. You can inspect logs with:
```sh
podman compose logs -f
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

### 4. Keycloak Authentication Service

The `keycloak` container provides OpenID Connect authentication for local development. It is initialized with realm `tasktally`, a confidential client `task-tally-backend` (API audience) and a public client `task-tally-swagger` for Swagger‑UI.

- **Admin credentials:**
  - Username: `admin`
  - Password: `admin`
- **Realm:** `tasktally`
- **Clients:**
  - `task-tally-backend` – service, bearer-only
  - `task-tally-swagger` – public, redirect `http://localhost:8080/q/swagger-ui/oauth2-redirect.html`
- **Test users:** `alice`/`alice` (role `user`), `admin`/`admin` (roles `user,admin`)

Access the admin console at:

```
http://localhost:8080
```

Login with the admin credentials above.

## Auth with Keycloak (dev)

```
# 1) Start services
podman compose up -d

# 2) Run the API
./mvnw quarkus:dev

# 3) Get a token (alice)
ACCESS_TOKEN=$(curl -s -X POST 'http://localhost:8080/realms/tasktally/protocol/openid-connect/token' \
  -H 'content-type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=task-tally-swagger&username=alice&password=alice' | jq -r .access_token)

# 4) Call the API
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/preferences/me
```

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
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/preferences/me

# Upsert preferences
curl -X PUT -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"ui":{"theme":"dark"}}' http://localhost:8080/api/preferences/me

# Link SSH credential (optional; defaults to `~/.ssh/id_rsa`)
curl -X POST -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"ssh","ref":"k8s:secret/mysecret#id_ed25519","scope":"write"}' http://localhost:8080/api/git/link

# Pull templates via SSH
curl -X POST -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"repoUri":"git@github.com:o/r.git","path":"templates","branch":"main"}' \
  http://localhost:8080/api/git/templates/pull
```

## Using SSH Keys

Task‑tally can upload SSH keys and keep the sensitive bytes in a
secret store. Postgres stores only references.

### Automatic Host Key Fetching (NEW)

The system now supports automatic SSH host key fetching when creating SSH credentials. Instead of manually running `ssh-keyscan`, you can simply specify the hostname and the system will automatically fetch the host keys.

#### 1. **Upload an existing key with automatic host key fetching**
   ```bash
   curl -X POST $BASE/api/users/user123/ssh-keys \
     -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" \
     -d '{"name":"my-gh-key","provider":"github",\
          "privateKeyPem":"-----BEGIN OPENSSH PRIVATE KEY-----\\n...\\n-----END OPENSSH PRIVATE KEY-----\\n",\
          "hostname":"github.com"}'
   ```

#### 2. **Generate a new key with automatic host key fetching**
   ```bash
   curl -X POST $BASE/api/users/user123/ssh-keys/generate \
     -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" \
     -d '{"name":"my-gh-key","provider":"github","hostname":"github.com"}'
   ```

#### 3. **Test host key fetching**
   ```bash
   # Fetch host keys from a server
   curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     $BASE/api/ssh/host-keys/github.com
   
   # Check if SSH service is available
   curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     $BASE/api/ssh/host-keys/github.com/check
   ```

### Manual Host Key Management (Legacy)

If you prefer to manage host keys manually or need to use custom host keys:

#### 1. **Upload an existing key with manual known_hosts**
   ```bash
   curl -X POST $BASE/api/users/user123/ssh-keys \
     -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" \
     -d '{"name":"my-gh-key","provider":"github",\
          "privateKeyPem":"-----BEGIN OPENSSH PRIVATE KEY-----\\n...\\n-----END OPENSSH PRIVATE KEY-----\\n",\
          "knownHosts":"github.com ssh-ed25519 AAAA...\\n"}'
   ```

#### 2. **Provide known_hosts manually**
   ```bash
   ssh-keyscan -t ed25519 github.com >> known_hosts
   ```

#### 3. **Register the public key** with GitHub/GitLab as a deploy key (allow write).

#### 4. **Validate and delete**
   ```bash
   curl -X POST $BASE/api/git/ssh/validate -H "Content-Type: application/json" \
     -H "Authorization: Bearer $ACCESS_TOKEN" -d '{"provider":"github","owner":"acme","repo":"templates","branch":"main","credentialName":"my-gh-key"}'

   curl -X DELETE $BASE/api/users/user123/ssh-keys/my-gh-key -H "Authorization: Bearer $ACCESS_TOKEN"
  ```

### Host Key Fetching Details

The automatic host key fetching feature:

- **Connects to SSH port 22** on the specified hostname
- **Fetches all available host keys** (ed25519, rsa, ecdsa, etc.)
- **Validates key format** and ensures they're properly formatted
- **Handles connection timeouts** gracefully (10 second connection timeout, 5 second read timeout)
- **Works with any SSH server** that supports the standard SSH protocol
- **Is secure** - only reads host keys, never attempts authentication

### Supported Hosts

The system works with any SSH server, but has been tested with:
- **GitHub** (`github.com`)
- **GitLab** (`gitlab.com`)
- **Bitbucket** (`bitbucket.org`)
- **Custom Git servers** (any hostname with SSH service on port 22)

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
curl -X POST $BASE/api/preferences/me/credentials   -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN"   -d '{
      "name":"my-gh-deploy-key",
      "provider":"github",
      "scope":"write",
      "secretRef":"k8s:secret/tasktally-git-user123#id_ed25519",
      "knownHostsRef":"k8s:secret/tasktally-git-user123#known_hosts"
    }'
```

6. **Validate SSH**
```bash
curl -X POST $BASE/api/git/ssh/validate   -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN"   -d '{"provider":"github","owner":"acme","repo":"templates","branch":"main","credentialName":"my-gh-deploy-key"}'
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

## Pre-commit Validation

Before committing code, run the `pre-commit.sh` script to validate your changes. This script checks formatting, runs tests, and ensures code quality standards are met.

To use it automatically before each commit, set up a Git pre-commit hook:

```sh
ln -s ../../pre-commit.sh .git/hooks/pre-commit
```

Or, run manually:

```sh
./pre-commit.sh
```

If the script fails, fix any reported issues before committing.
