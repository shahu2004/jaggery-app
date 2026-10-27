# Jaggery App — Marketplace MVP

A portfolio project for a jaggery marketplace using Java 21, Spring Boot, Spring Security, JWT, Spring Data JPA, MySQL, Spring Cloud Gateway, Eureka and React/Vite.

> **Status:** MVP under active development on `feature/mvp-completion-roadmap`. The backend services, initial cart/order flow and frontend have been added on this branch. Run the latest GitHub Actions checks and manually verify the complete flow before merging or deploying publicly. Payment processing is simulated; this app does not take real payments.

## Architecture

| Component | Local port | Purpose |
|---|---:|---|
| React/Vite frontend | 5173 | Catalog, sign-in, cart, test checkout, order history, seller studio and admin pages |
| API Gateway | 8080 | Routes `/api/**` requests to the services and handles development CORS |
| Auth Service | 8081 | Registration, login, BCrypt password hashing, JWT and user/seller administration |
| Product Service | 8082 | Product/category APIs, search, type/price/category filters, pagination, sorting, soft delete and image upload |
| Order Service | 8083 | User cart, server-priced checkout snapshots, order history, cancellation and order status |
| Eureka Discovery Server | 8761 | Service registry |
| MySQL | 3306 | Separate `jaggery_auth`, `jaggery_product` and `jaggery_order` databases |

## Run all components locally (recommended)

### Prerequisites

- Docker Desktop with Docker Compose
- Git and this repository
- A browser

### Start

1. Copy `.env.example` to `.env` in the repository root.
2. Replace `MYSQL_ROOT_PASSWORD` with a local password and `JWT_SECRET` with a random secret of at least 32 bytes. Do not commit `.env`.
3. From the repository root, run:

```bash
docker compose config
docker compose up --build
```

4. Open:
   - Frontend: http://localhost:5173
   - API Gateway: http://localhost:8080
   - Eureka dashboard: http://localhost:8761

MySQL is bound to localhost only. The backend service ports are internal to the Compose network; only the frontend, gateway and local Eureka dashboard are published. Docker volumes preserve the local database and uploaded images.

### Useful commands

```bash
docker compose ps
docker compose logs -f auth-service product-service order-service api-gateway
docker compose down
```

`docker compose down -v` deletes the local database and image volumes. Use it only when you intentionally want to erase the local data.

## Local administrator setup

Public registration creates a `CUSTOMER` account. To test admin features locally, follow [docs/LOCAL_ADMIN_BOOTSTRAP.md](docs/LOCAL_ADMIN_BOOTSTRAP.md). This uses a local-only database procedure for development; do not expose MySQL publicly or use that manual process in production.

Admin users can approve customers as sellers through `POST /api/users/{id}/approve-seller`. After role approval, the user must sign in again to receive a JWT with the updated role.

## Frontend

See [frontend/README.md](frontend/README.md). The frontend defaults to `VITE_API_BASE_URL=http://localhost:8080`. For a deployed frontend, set `VITE_API_BASE_URL` to the public HTTPS gateway URL **at build time**, and set `FRONTEND_ORIGIN` to the exact frontend origin.

Available frontend flows:
- Product catalog, search, filters, sorting and pagination
- Register/login
- Cart and simulated checkout using server-read product prices
- Order history and cancellation while status is `PLACED`
- Seller product CRUD and image upload for approved sellers
- Admin category management and seller approval

## Existing API endpoints

### Auth Service (through gateway at port 8080)

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users` — admin-only
- `POST /api/users/{id}/approve-seller` — admin-only

### Product Service

- `GET /api/products` — supports page, size, search, type, minPrice, maxPrice, categoryId, sortBy and direction
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}` — soft delete
- `POST /api/products/{id}/image`
- `GET /api/products/images/{filename}`
- `GET /api/categories`
- `POST /api/categories` — admin-only

### Order Service

All cart/order endpoints require a valid bearer token:
- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{productId}`
- `DELETE /api/cart/items/{productId}`
- `POST /api/orders/checkout` — test checkout, no payment is collected
- `GET /api/orders`
- `GET /api/orders/{id}` — only the owning customer can read their order
- `POST /api/orders/{id}/cancel` — cancellation allowed while `PLACED`
- `PATCH /api/orders/{id}/status` — admin-only permitted transitions

Use the gateway URL from the browser frontend; direct localhost service ports are useful for local debugging only.

## Tech stack

- Java 21
- Spring Boot / Spring Cloud
- Spring Security, JWT and BCrypt
- Spring Data JPA / Hibernate
- MySQL
- React, Vite and CSS
- Maven Wrapper, JUnit, Mockito and GitHub Actions
- Docker and Docker Compose

## Security and production readiness

The completion branch validates bearer JWTs in Product Service and Order Service. Product write endpoints derive user ID/role from the verified JWT principal rather than trusting caller-supplied identity headers. Public registration assigns `CUSTOMER`, and seller approval is admin-only.

This still needs a full security review and integration testing. Before public deployment:
- Pass the latest CI workflow and complete manual end-to-end testing.
- Use TLS/HTTPS, rate limiting and platform-managed secrets; never use the sample secrets.
- Use a least-privilege database account per service instead of MySQL root.
- Keep Eureka, MySQL and backend ports private. Expose only intended gateway/frontend endpoints.
- Replace Hibernate `ddl-auto=update` with versioned database migrations and schema validation.
- Use persistent/object storage for images in the target hosting environment.
- Add inventory concurrency/stock checks, checkout idempotency and a real payment provider before accepting actual payments.
- Add broader HTTP integration tests, backups/restore validation and an audit trail for role/order changes.

## Development roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the phased implementation checklist and remaining work. Local deployment instructions are in [DEPLOYMENT.md](DEPLOYMENT.md), cloud guidance is in [docs/RENDER_RAILWAY_DEPLOYMENT.md](docs/RENDER_RAILWAY_DEPLOYMENT.md), and the smoke-test script is [scripts/verify-local.ps1](scripts/verify-local.ps1).
