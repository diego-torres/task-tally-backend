# AGENTS.md

Shared rules and reusable snippets for building **Task-tally** agents (e.g., Codex or other codegen/automation agents). Keep this file stable; update via PRs with clear rationale.

---

## 1) Mission & Scope
- **Mission:** Generate production-grade code and artifacts for Task-tally quickly, safely, and consistently.
- **Primary stack:** Quarkus (Java 17), PostgreSQL (Flyway), GitHub integration, Kubernetes runtime, React/PatternFly frontend.
- **Data model principle:** Non-secret user preferences live in Postgres; **secrets are never stored** in Postgres—only **references** to external secret stores.

---

## 2) Core Non-Negotiables (Security & Compliance)
1. **Never store raw secrets** (SSH keys, tokens, app private keys) in source control, logs, or databases. Store only **references** like `k8s:secret/<name>#<key>` or `vault:kv/...`.
2. **No plaintext secret echo** in logs, exceptions, or tests. Mask with `****`.
3. **SSH keys support (NEW):**
   - API accepts uploaded private keys only to **immediately store in an external secret store** (Kubernetes Secret or Vault).
   - DB persists only the reference string.
   - Backend services resolve keys at runtime via the `SecretResolver`.
4. **Encryption:** rely on secret manager/KMS; if temporary encryption needed, use AES-GCM with rotated keys (but avoid persisting secrets anyway).
5. **PII minimization:** store the minimum user info; never log user emails or tokens.
6. **Dependency hygiene:** pin versions; avoid unmaintained libraries.

---

## 3) Architectural Defaults
- **Backend:** Quarkus REST, layered: `api → service → repo → db`.
- **Persistence:** Postgres with Flyway migrations; JSONB for flexible prefs.
- **Secrets:** `SecretResolver` SPI (Kubernetes + Vault stub). Code consumes **only** secret values returned at runtime; DB stores **refs**.
- **Git:** 
  - Prefer GitHub REST API or App installation tokens.  
  - Allow **SSH transport** when user has uploaded an SSH key (resolved from secret store).
  - **Templates and Outcomes:** Stored as YAML files in Git repositories, not in database.
- **Kubernetes:** 12-factor config via env vars; health checks; readiness/liveness; graceful shutdown.

---

## 4) API Design Rules
- **New endpoint for SSH keys:**
  - `POST /api/preferences/me/ssh-keys` → upload SSH private key → backend writes to secret store, returns `credentialRef`.
  - `DELETE /api/preferences/me/ssh-keys/{name}` → remove reference.
- **Usage in Git ops:** Push/pull endpoints can specify `credentialName` pointing to an SSH key ref.

Other rules unchanged (see base version).

---

## 5) Data & Schema Rules (Postgres)
- Table `credential_refs` now supports `provider = 'SSH_KEY'`.
- Example row:  
  ```
  (userId=123, name='gitlab-ssh', provider='SSH_KEY', scope='repo', secretRef='k8s:secret/tasktally-ssh-123#id_rsa')
  ```
- **Templates:** Store Git repository references (URL, branch, SSH key name, YAML path).
- **Outcomes:** Stored in Git repositories as YAML files, not in database.
- **YAML Structure:** Outcomes follow the structure defined in the specification with phase objects containing name, track, product, and environment.

---

## 6) Secrets Handling Pattern
- **Reference format:** unchanged.
- **Resolver flow for SSH keys:**
  1. Parse `secretRef`.
  2. Resolve private key material at runtime.
  3. Instantiate JGit `SshSessionFactory` with in-memory key; never persist locally.
- **Forbidden:** Persisting SSH private key bytes in Postgres or writing to temp files outside container secret mounts.

---

## 10) Reusable Prompt Snippets (for codegen agents)

### H) SSH Key Integration (NEW)
```
Implement:
POST /api/preferences/me/ssh-keys {name, privateKeyPem}
→ store in Kubernetes/Vault secret, persist credentialRef row.
DELETE /api/preferences/me/ssh-keys/{name} → remove.
Update GitService to support SSH by resolving privateKeyPem via SecretResolver and wiring to JGit SshSessionFactory.
Ensure no key material is persisted in DB or logs.
```

### I) Git-based YAML Storage (NEW)
```
Templates and outcomes are stored in Git repositories as YAML files, not in database.
- Templates: Store Git repository references (URL, branch, SSH key name, YAML path).
- Outcomes: Stored as outcomes.yml files in Git repositories.
- YAML Structure: Follows the specification with phase objects containing name, track, product, environment.
- Git Operations: Use SshGitService for clone, commit, and push operations.
- No Database Repositories: Do not create database repositories for outcomes or templates data.
```

---

## 11) PR Checklist (for humans and agents)
- [ ] No secrets or secret values persisted, printed, or committed.
- [ ] SSH keys, if uploaded, are stored only in secret store and referenced in DB.
- [ ] Flyway migrations present and idempotent.
- [ ] JSON error envelope + request id wired.
- [ ] Unit and resource tests pass locally.
- [ ] Health/readiness endpoints OK.
- [ ] README updated with new SSH key endpoints and example curls.

## Formatting Rules

- **Java files:** Use 2 spaces for indentation throughout all `.java` files. No tabs allowed.

---

*End of AGENTS.md*


