# Jaggery App

A Java 21 / Spring Boot backend for a jaggery marketplace. This repository is under active development; the backend services exist, while the end-to-end marketplace experience is still being completed.

## Current architecture

| Service | Local port | Responsibility |
|---|---:|---|
| Discovery Server (Eureka) | 8761 | Registers and discovers backend services |
| Auth Service | 8081 | Registration, login, BCrypt password hashing, JWT issuance, user profile APIs and role checks |
| Product Service | 8082 | Product/category APIs, search, filters, pagination, sorting, soft delete and image uploads |

Current code is in `backend/`. A public API Gateway, React frontend and cart/order service are planned work, not yet included in this repository tree.

## Tech stack

- Java 21
- Spring Boot and Spring Cloud Netflix Eureka
- Spring Security and JSON Web Tokens (JWT)
- Spring Data JPA / Hibernate
- MySQL
- Maven Wrapper
- JUnit / Spring Boot test starters (the existing tests are currently basic context-load tests)

## Existing API surface

### Auth Service — `http://localhost:8081`

- `GET /api/auth/ping` — service status
- `POST /api/auth/register` — register a user
- `POST /api/auth/login` — authenticate and receive a bearer token
- `GET /api/users/me` — retrieve the authenticated user's profile
- `GET /api/users` — administrator-only user listing

### Product Service — `http://localhost:8082`

- `GET /api/products` — paginated product list with optional search, type, minimum/maximum price, category and sort parameters
- `GET /api/products/{id}` — product details
- `POST /api/products` — create a product
- `PUT /api/products/{id}` — update a product
- `DELETE /api/products/{id}` — soft-delete a product
- `POST /api/products/{id}/image` — upload a product image
- `GET /api/products/images/{filename}` — retrieve an uploaded image
- `GET /api/categories` — list categories
- `POST /api/categories` — create a category

For write APIs, inspect the source and the security notes below before exposing them outside localhost.

## Local prerequisites

- JDK 21
- MySQL running locally
- Create databases named `jaggery_auth` and `jaggery_product`
- PowerShell on Windows, or a shell that can run the Maven Wrapper

The services read configuration from environment variables. Common variables include:

- Auth Service: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `EUREKA_URL`
- Product Service: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DDL_AUTO`, `EUREKA_URL`, `UPLOAD_DIR`

Use a random JWT secret of at least 32 bytes for local development. Never commit actual passwords, JWT secrets, production credentials or tokens.

### Run locally on Windows

Open three PowerShell terminals from the repository root.

**1. Eureka Discovery Server**
```powershell
cd backend\discovery-server\discovery-server
.\mvnw.cmd spring-boot:run
```

**2. Auth Service**
Set the environment variables before starting the service. Point `DB_URL` to the `jaggery_auth` database and `JWT_SECRET` to a secure local-only value.
```powershell
cd backend\auth-service\auth-service
$env:DB_URL = "jdbc:mysql://localhost:3306/jaggery_auth?useSSL=false&serverTimezone=Asia/Kolkata"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-local-mysql-password>"
$env:JWT_SECRET = "<a-random-secret-of-at-least-32-bytes>"
$env:EUREKA_URL = "http://localhost:8761/eureka"
.\mvnw.cmd spring-boot:run
```

**3. Product Service**
Use the `jaggery_product` database and set its own database environment variables.
```powershell
cd backend\product-service\product-service
$env:DB_URL = "jdbc:mysql://localhost:3306/jaggery_product?useSSL=false&serverTimezone=Asia/Kolkata"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-local-mysql-password>"
$env:EUREKA_URL = "http://localhost:8761/eureka"
.\mvnw.cmd spring-boot:run
```

Replace placeholders with local values. Do not paste secrets into source files. If the Maven Wrapper fails, check that JDK 21 is selected and that Maven Wrapper files are present.

## Security and readiness notes

This project is a work in progress and must not be treated as production-ready until these items are addressed:

1. Product Service currently accepts `X-User-Id` and `X-User-Role` headers for write operations. Do not trust caller-supplied identity headers as authentication. Validate JWTs or establish a properly secured gateway-to-service trust boundary, and prevent direct untrusted access to the service.
2. Review public registration so callers cannot elevate their privileges by choosing a role. Seller onboarding should follow an explicit, authorized policy.
3. Add real unit and integration tests for authentication, authorization, product CRUD, filtering, pagination, categories and image uploads.
4. Replace `ddl-auto: update` with controlled database migrations before production deployment.
5. Use persistent image storage when hosting on an ephemeral filesystem.
6. Store credentials and secrets in environment/secret-manager configuration only.

## Development plan

See [docs/ROADMAP.md](docs/ROADMAP.md) for the ordered completion plan. The priority order is: baseline builds and API checks, security fixes, tests, API Gateway, cart/order service, React frontend, deployment, and documentation/demo polish.
