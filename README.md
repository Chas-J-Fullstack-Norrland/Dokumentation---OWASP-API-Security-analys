# Library API

Spring Boot 4 REST API for managing authors, books, and loans.

## Tech stack
- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Data JPA (H2)
- Spring Security + JWT
- Spring Validation
- Spring Cache
- Spring Cloud Vault (`spring-cloud-starter-vault-config`)
- Bucket4j (rate limiting)
- SpringDoc OpenAPI / Swagger UI
- Maven

## Main features
- Versioned API (`/api/v1/...` and `/api/v2/...`)
- Validation + centralized error responses
- JWT auth endpoints (`/api/auth/login`, `/api/auth/refresh`)
- Loan concurrency protection in service/repository layer
- Caching for book endpoints
- Per-IP rate limiting
- Secrets loaded from Vault (JWT signing secret)

## Vault integration (new)

`app.jwt.secret` is no longer expected in `application.properties`.
It is read from Vault using these config entries:

- `spring.config.import=vault://`
- `spring.cloud.vault.uri=http://127.0.0.1:8200`
- `spring.cloud.vault.token=root`
- `spring.cloud.vault.kv.backend=secret`
- `spring.cloud.vault.kv.default-context=library`

Expected secret path and key:

- Path: `secret/library`
- Key: `app.jwt.secret`

The secret value must be a valid Base64 string (32-byte key encoded to Base64 is recommended).

## Quick start (Windows PowerShell)

### Option A: One-command dev bootstrap (recommended)

This script starts Vault dev server if needed, ensures `app.jwt.secret` exists, and starts Spring Boot:

```powershell
.\start-dev.ps1
```

Rotate JWT secret and restart:

```powershell
.\start-dev.ps1 -RotateSecret
```

### Option B: Manual Vault + app startup

Start Vault dev server:

```powershell
vault server -dev -dev-root-token-id="root"
```

In another terminal, set Vault env vars and write the secret:

```powershell
$env:VAULT_ADDR = "http://127.0.0.1:8200"
$env:VAULT_TOKEN = "root"
vault kv put secret/library app.jwt.secret="<BASE64_SECRET>"
```

Start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

## Useful URLs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

## Run tests
```powershell
.\mvnw.cmd test
```

## Cache results before and after 
-- in .Benchmark-results.json file 



## Troubleshooting Vault
- `No value found at secret/data/library`: re-run `vault kv put secret/library app.jwt.secret="<BASE64_SECRET>"`.
- `Illegal base64 character`: `app.jwt.secret` is not valid Base64; generate and store a valid Base64 secret.
- App fails on Vault connection: verify Vault is running at `http://127.0.0.1:8200` and token is `root` for dev mode.

