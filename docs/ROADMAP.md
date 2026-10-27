# Jaggery App MVP Completion Roadmap

**Goal:** deliver a demoable jaggery marketplace with secure authentication, product discovery, seller management, cart/checkout, order history, tests and deployment.

## Status snapshot (reviewed 2026-10-27)

### Already in repository
- Eureka Discovery Server on local port 8761.
- Auth Service on local port 8081: registration, login, BCrypt password hashing, JWT issuance, user profile endpoint, basic role handling.
- Product Service on local port 8082: product/category endpoints, search, filtering, pagination, sorting, soft delete and image upload/retrieval.
- Maven Wrapper included in each service.
- Application configuration supports environment variables for database, JWT and Eureka settings.

### Not yet present in repository tree
- Public API Gateway
- React frontend
- Cart and order service
- Root README (being added in this workstream)
- GitHub Actions workflow
- Meaningful automated tests beyond context-load tests

## Delivery sequence

### Phase 0 — Baseline and documentation
- [ ] Build each service with its Maven Wrapper.
- [ ] Start MySQL and create jaggery_auth and jaggery_product.
- [ ] Start Eureka, Auth Service and Product Service.
- [ ] Save working and failing API calls in a Postman collection.
- [ ] Record baseline build/test output without hiding failures.

**Exit criteria:** all known build failures and configuration blockers are written down.

### Phase 1 — Security before features (highest priority)
- [ ] Stop trusting caller-controlled X-User-Id and X-User-Role values as identity proof.
- [ ] Validate JWT signatures and expiry at the Product Service boundary, or implement a verified trusted gateway boundary and prevent direct service access.
- [ ] Derive user identity and roles only from verified authentication.
- [ ] Keep public registration from assigning privileged roles; define seller approval policy.
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
- [ ] Add Spring Cloud Gateway on port 8080 with versions compatible with the existing Spring Boot/Cloud line.
- [ ] Route /api/auth/** and /api/users/** to Auth Service.
- [ ] Route /api/products/** and /api/categories/** to Product Service.
- [ ] Configure frontend CORS origins deliberately.
- [ ] Keep internal service ports private in deployed environments.
- [ ] Add health checks and avoid logging passwords or bearer tokens.

**Exit criteria:** frontend APIs work through the gateway and security is unchanged by routing.

### Phase 4 — Cart and Order Service
- [ ] Create a separate Spring Boot service and MySQL database for cart/order data.
- [ ] Add cart and cart-item models, repository, service and APIs.
- [ ] Validate product existence and price from trusted server-side product data.
- [ ] Create order and order-item snapshots with immutable checkout prices.
- [ ] Implement order history and controlled status transitions.
- [ ] Prevent negative stock/overselling if inventory is added.
- [ ] Start with test/simulated payment state before integrating a real payment provider.
- [ ] Add tests for empty cart, invalid quantity, repeated requests, service failures and cross-user access.

**Exit criteria:** a customer can place a test order and later read their own order history.

### Phase 5 — React frontend
- [ ] Set up React with Vite and React Router.
- [ ] Build product listing, details, search, filtering and pagination pages.
- [ ] Build registration, login and authenticated session handling.
- [ ] Build seller product management and image upload.
- [ ] Build cart, checkout, order history and order status views.
- [ ] Build admin category/management views as required.
- [ ] Keep permissions enforced server-side; protected/hide-only frontend routes are not security.
- [ ] Handle loading, empty, success and error states consistently.

**Exit criteria:** users can complete the marketplace MVP end to end from the UI.

### Phase 6 — Quality, delivery and deployment
- [ ] Add meaningful JUnit/Mockito unit tests and HTTP integration tests.
- [ ] Add GitHub Actions to build and test the services on pushes and pull requests.
- [ ] Add sample environment file with placeholders only; never commit real secrets.
- [ ] Use database migrations and least-privilege DB users.
- [ ] Use persistent or object storage for product images on cloud deployments.
- [ ] Deploy each service, gateway, database and frontend with private network/service settings where possible.
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

## Current critical risk
The Product Service currently receives X-User-Id and X-User-Role headers on write operations. Caller-supplied headers are not proof of identity. Fix and test this before exposing the service beyond trusted local development.
