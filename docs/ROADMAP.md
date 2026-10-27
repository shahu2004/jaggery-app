# Jaggery App MVP Completion Roadmap

**Goal:** deliver a demoable jaggery marketplace with secure authentication, product discovery, seller management, cart/checkout, order history, tests and deployment.

## Status snapshot (reviewed 2026-10-27)

### Already in repository
- Eureka Discovery Server on local port 8761.
- Auth Service on local port 8081: registration, login, BCrypt password hashing, JWT issuance, user profile endpoint, basic role handling.
- Product Service on local port 8082: product/category endpoints, search, filtering, pagination, sorting, soft delete and image upload/retrieval.
- Maven Wrapper included in each service.
- Application configuration supports environment variables for database, JWT and Eureka settings.

### Added on this branch (latest full CI pending)
- API Gateway on port 8080 with Eureka routes and development CORS
- Order Service initial implementation: cart, checkout snapshot, order history, cancellation and status workflow
- React/Vite frontend with catalog, auth, cart, test checkout, order history, seller tools and admin seller approvals
- Dockerfiles, Docker Compose local stack and deployment notes

### Remaining work
- Real payment provider integration (only after order flow is reliable)
- Production email notifications, inventory/stock tracking and audit trail
- HTTP integration/security tests for the complete end-to-end flow
- Production secrets, database migrations, private networking and image object storage
- Live deployment and manual browser-based smoke tests

## Delivery sequence

### Phase 0 — Baseline and documentation
- [ ] Build each service with its Maven Wrapper.
- [ ] Start MySQL and create jaggery_auth and jaggery_product.
- [ ] Start Eureka, Auth Service and Product Service.
- [ ] Save working and failing API calls in a Postman collection.
- [ ] Record baseline build/test output without hiding failures.

**Exit criteria:** all known build failures and configuration blockers are written down.

### Phase 1 — Security before features (highest priority)
- [x] Stop trusting caller-controlled X-User-Id and X-User-Role values as identity proof.
- [x] Validate JWT signatures and expiry at the Product Service boundary.
- [x] Derive product write identity and roles from the verified principal.
- [x] Public registration assigns CUSTOMER only; seller approval is administrator-only.
- [ ] Test unauthenticated, invalid-token, expired-token and incorrect-role behavior.
- [ ] Confirm sellers can modify only their own products and only admins can manage categories.

**Exit criteria:** security tests demonstrate that caller-supplied headers cannot impersonate a privileged user.

### Phase 2 — Product-service quality
- [ ] Add unit tests for product creation, update, soft delete, ownership and category rules.
- [ ] Add API tests for search, filtering, pagination, sorting and invalid parameters.
- [ ] Test image upload validation, oversized requests, retrieval and deletion behavior.
- [ ] Add email validation to registration and test malformed request data.
- [ ] Replace schema auto-update with database migrations before production.

**Exit criteria:** core product behaviors are covered by reproducible automated tests.

### Phase 3 — API Gateway
- [x] Add Spring Cloud Gateway on port 8080 with versions compatible with the existing Spring Boot/Cloud line.
- [x] Route /api/auth/** and /api/users/** to Auth Service.
- [x] Route /api/products/** and /api/categories/** to Product Service.
- [x] Configure frontend CORS origins deliberately for local development.
- [ ] Keep internal service ports private in deployed environments.
- [ ] Add health checks and avoid logging passwords or bearer tokens.

**Exit criteria:** frontend APIs work through the gateway and security is unchanged by routing.

### Phase 4 — Cart and Order Service (initial implementation added on this branch; verification pending)
- [x] Create a separate Spring Boot service and MySQL database for cart/order data.
- [x] Add cart and cart-item models, repository, service and APIs.
- [ ] Validate product existence and price from trusted server-side product data.
- [x] Create order and order-item snapshots using server-read product prices at checkout.
- [x] Implement order history, customer cancellation before confirmation, and controlled status transitions.
- [ ] Prevent negative stock/overselling if inventory is added.
- [ ] Start with test/simulated payment state before integrating a real payment provider.
- [x] Add unit tests for checkout totals/server-sourced prices, empty carts, order ownership, invalid status transitions, cart quantity increments and limits.
- [ ] Add HTTP integration tests, repeated-checkout/idempotency protection and explicit upstream failure tests. Current context-load test is not sufficient.

**Exit criteria:** a customer can place a test order and later read their own order history.

### Phase 5 — React frontend (initial implementation added on this branch; verification pending)
- [x] Set up React with Vite.
- [x] Build a product listing with search, filtering, and sorting (pagination UI is still pending).
- [x] Build registration/login screens and bearer-token session handling.
- [x] Build seller product CRUD and image upload screens (requires approved seller role).
- [x] Build cart, simulated checkout, order history and cancellation views.
- [x] Build administrator category creation view.
- [ ] Keep permissions enforced server-side; protected/hide-only frontend routes are not security.
- [x] Add loading, empty, success and error states.

**Exit criteria:** users can complete the marketplace MVP end to end from the UI.

### Phase 6 — Quality, delivery and deployment
- [ ] Add meaningful JUnit/Mockito unit tests and HTTP integration tests.
- [ ] Verify GitHub Actions builds/tests all services and the frontend on pushes and pull requests.
- [x] Add sample environment file with placeholders only; never commit real secrets.
- [ ] Use database migrations and least-privilege DB users.
- [ ] Use persistent or object storage for product images on cloud deployments.
- [ ] Deploy each service, gateway, database and frontend with private network/service settings where possible (see [cloud deployment checklist](RENDER_RAILWAY_DEPLOYMENT.md)).
- [ ] Verify registration → login → browse → seller product CRUD → cart → test checkout → order history.
- [ ] Document architecture, ports, configuration, API examples and screenshots in README.

**Exit criteria:** a new developer can set up the app from documentation and the deployed demo passes the end-to-end checklist.

## Suggested 30-day schedule

| Days | Target |
|---|---|
| 1–3 | Baseline builds, databases, API/Postman verification |
| 4–7 | Authentication boundary and authorization fixes |
| 8–10 | Gateway and service routing |
| 11–14 | Product validation and meaningful tests |
| 15–19 | Cart and Order Service |
| 20–24 | React frontend |
| 25–30 | Test suite, CI, deployment, documentation and demo |

This schedule is an estimate; do not move to the next phase until the current phase's exit criteria are met.

## Current critical risks
- Latest full CI is still being verified. Do not merge/deploy until it passes.
- The local Compose configuration is not a production hardening setup; it uses a root DB account and Hibernate schema auto-update.
- Order checkout currently has no stock reservation or idempotency protection and does not process payments.
