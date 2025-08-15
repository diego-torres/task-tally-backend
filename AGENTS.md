# AGENTS.md

Shared rules and reusable snippets for building **Task‑tally** agents (e.g., Codex or other codegen/automation agents). Keep this file stable; update via PRs with clear rationale.

---

## 1) Mission & Scope
- **Mission:** Generate production‑grade code and artifacts for Task‑tally quickly, safely, and consistently.
- **Primary stack:** Quarkus (Java 17), PostgreSQL (Flyway), GitHub integration, Kubernetes runtime, React/PatternFly frontend.
- **Data model principle:** Non‑secret user preferences live in Postgres; **secrets are never stored** in Postgres—only **references** to external secret stores.

---

## 2) Core Non‑Negotiables (Security & Compliance)
1. **Never store raw secrets** (SSH keys, tokens, app private keys) in source control, logs, or databases. Store only **references** like `k8s:secret/<name>#<key>` or `vault:kv/...`.
2. **No plaintext secret echo** in logs, exceptions, or tests. Mask with `****`.
3. Prefer **GitHub App** installation tokens over PATs; avoid user‑supplied SSH private keys.
4. **Encryption:** rely on secret manager/KMS; if temporary encryption needed, use AES‑GCM with rotated keys (but avoid persisting secrets anyway).
5. **PII minimization:** store the minimum user info; never log user emails or tokens.
6. **Dependency hygiene:** pin versions; avoid unmaintained libraries.

---

## 3) Architectural Defaults
- **Backend:** Quarkus REST, layered: `api → service → repo → db`.
- **Persistence:** Postgres with Flyway migrations; JSONB for flexible prefs.
- **Secrets:** `SecretResolver` SPI (Kubernetes + Vault stub). Code consumes **only** secret values returned at runtime; DB stores **refs**.
- **Git:** Prefer GitHub REST API; use JGit only if needed for multi‑file batch edits.
- **Kubernetes:** 12‑factor config via env vars; health checks; readiness/liveness; graceful shutdown.

---

## 4) API Design Rules
- **Versioning:** `/api` base; add `/v1` when breaking changes accrue.
- **Auth (MVP):** `X-User-Id` header; return `401` if missing.
- **Idempotency:** `PUT` for upserts; `POST` for actions; `DELETE` idempotent.
- **Errors:** JSON error envelope with `code`, `message`, `requestId`.
- **Validation:** Use `jakarta.validation` annotations; return `400` with field messages.
- **Pagination:** `?page`,`?size` with sensible defaults.

**Error JSON example**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "name must not be blank",
  "requestId": "c7d1f1f4-..."
}
```

---

## 5) Data & Schema Rules (Postgres)
- Use **Flyway**; never mutate tables without a migration.
- **Tables:**
  - `user_preferences(user_id UNIQUE, ui JSONB, default_git_provider, version, created_at, updated_at)`
  - `credential_refs(user_preferences_id FK, name UNIQUE per user, provider ENUM-like check, scope check, secret_ref)`
- Add GIN index for JSONB fields you query.
- Use triggers to maintain `updated_at`.

---

## 6) Secrets Handling Pattern
- **Reference format:** `k8s:secret/<name>#<key>` → mounted file or env value.
- **Resolver flow:** parse ref → read value → return string → consumer builds short‑lived token.
- **GitHub App flow:** `{ appId, installationId, privateKeyPem }` → sign JWT → exchange for installation token → cache until expiry.
- **PAT flow:** `{ token }` → use directly, no persistence.

**Forbidden:** Persisting `privateKeyPem` or PAT tokens in DB.

---

## 7) Logging & Observability
- Add a `RequestIdFilter` that sets MDC `requestId` and returns it as `X-Request-Id`.
- Log at INFO for start/stop, DEBUG for diagnostics; never log payloads containing secrets.
- Health endpoints: `/q/health`, `/q/health/ready`, `/q/health/live`.
- Metrics: Micrometer/MP Metrics default; label with low‑cardinality tags only.

---

## 8) Testing Strategy
- **Pyramid:** unit > resource/integration > e2e.
- Unit test services and validators; mock GitHub HTTP calls.
- Flyway migration tests: start ephemeral Postgres (Testcontainers) and assert schema.
- Resolver tests: parse and map `k8s:secret/*` refs; never require real secrets.

---

## 9) Kubernetes & Config Conventions
- Config strictly via **environment variables** (12‑factor). Examples:
  - `DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `GITHUB_API_BASE` (default `https://api.github.com`)
  - `K8S_SECRET_BASE_PATH` (default `/var/run/secrets/...`)
- Expose readiness/liveness probes; graceful shutdown within 30s.
- Use a **separate** Postgres deployment/managed service (never sidecar DB in the API container).

---

## 10) Reusable Prompt Snippets (for codegen agents)

### A) Project Bootstrap (Quarkus + Postgres + Flyway)
```
Create a Quarkus (Java 17) project with Maven coordinates io.redhat.na.ssp:task-tally-backend.
Add dependencies: quarkus-resteasy-reactive-jackson, quarkus-hibernate-orm-panache, quarkus-jdbc-postgresql, quarkus-flyway, quarkus-arc, quarkus-hibernate-validator, quarkus-junit5, rest-assured, snakeyaml, nimbus-jose-jwt.
Configure runtime via environment variables only; no secrets or URLs hardcoded.
Generate Flyway migrations for user_preferences and credential_refs per AGENTS.md.
```

### B) Entities & Repos (JSONB + optimistic locking)
```
Implement UserPreferences(id, userId UNIQUE, ui JSONB as Map<String,Object>, defaultGitProvider, version @Version, createdAt, updatedAt) and CredentialRef(id, userPreferences FK, name, provider, scope, secretRef, createdAt).
Use Panache repositories; add JSONB mapping; write Flyway SQL with indexes and triggers per AGENTS.md.
```

### C) REST Endpoints & Validation
```
Implement:
GET /api/preferences/me → by X-User-Id; 401 if missing.
PUT /api/preferences/me → upsert with Bean Validation.
POST /api/preferences/me/credentials → add CredentialRef (store only secretRef string).
DELETE /api/preferences/me/credentials/{name} → remove by name.
Return JSON error envelope with code/message/requestId.
```

### D) SecretResolver SPI
```
Create SecretResolver interface with resolve(String ref): String.
Implement KubernetesSecretResolver reading k8s:secret/<name>#<key> from mounted files or env; add VaultSecretResolver stub throwing UnsupportedOperationException.
Unit test ref parsing and file reads; never print secret values.
```

### E) GitHub Integration
```
Prefer GitHub App. Implement a helper to build a JWT from appId + privateKeyPem and exchange for an installation token (cache until expiry). Provide fallback PAT support.
Expose endpoints:
POST /api/github/link {name, ref}
POST /api/github/templates/pull {owner, repo, path, branch, credentialName}
POST /api/github/templates/push {owner, repo, path, branch, files:[{name,yaml}], message, credentialName}
Use SnakeYAML for validation of templates (ProjectTemplate, Activity).
```

### F) Error & Log Hygiene
```
Add RequestIdFilter, structured logs, and central exception mapper. Never include secretRef contents or any secret material in logs.
```

### G) Testing Harness
```
Use @QuarkusTest. Testcontainers for Postgres with Flyway. Mock GitHub calls. Add tests for optimistic locking and SecretResolver parsing.
```

---

## 11) PR Checklist (for humans and agents)
- [ ] No secrets or secret values persisted, printed, or committed.
- [ ] Flyway migrations present and idempotent.
- [ ] JSON error envelope + request id wired.
- [ ] Unit and resource tests pass locally.
- [ ] Health/readiness endpoints OK.
- [ ] README updated with env vars and example curls.

---

## 12) Naming & Style
- Package root: `io.redhat.na.ssp.tasktally`.
- Resource class names end with `Resource`; services end with `Service`.
- Test names end with `Test`; use given/when/then naming.
- Keep methods ≤ 40 lines; prefer composition over inheritance.

---

## 13) Non‑Goals
- No auth provider integration (OIDC) in MVP.
- No secret managers embedded; only resolvers to external stores.
- No DB‑embedded or in‑container Postgres.

---

## 14) Glossary
- **SecretRef:** An opaque pointer to a secret location (K8s or Vault). Not the secret value.
- **Resolver:** Component that translates SecretRef → secret value at runtime.
- **Template:** YAML document describing reusable project/task structures.

---

*End of AGENTS.md*

