# Jaggery App — deployment guide

## Run the whole project locally with Docker Compose

This Compose configuration is for local development/demo, not a production deployment.

1. Install Docker Desktop and verify Docker Compose works.
2. From the repository root, copy `.env.example` to `.env`.
3. Set `MYSQL_ROOT_PASSWORD` and a private random `JWT_SECRET` of at least 32 bytes. All three JWT-aware services (Auth, Product, Order) must use the same secret.
4. Validate the Compose file and build all service images (the same image build is checked in CI):

   ```bash
   docker compose config
   docker compose up --build
   ```

5. Open the frontend at http://localhost:5173, API Gateway at http://localhost:8080 and Eureka dashboard at http://localhost:8761.

Useful commands:

```bash
docker compose ps
docker compose logs -f auth-service product-service order-service api-gateway
docker compose down
```

The named volumes preserve MySQL data and uploaded product images across container replacement. `docker compose down -v` deletes them; only use it when you deliberately want to erase local data.

## First administrator for local testing

Public registration creates CUSTOMER accounts. Follow [docs/LOCAL_ADMIN_BOOTSTRAP.md](docs/LOCAL_ADMIN_BOOTSTRAP.md) to promote a local development account to ADMIN with local-only SQL. Do not expose MySQL publicly or use the manual development procedure in production.

## Before public/production deployment

- Use platform secret settings; do not reuse values from `.env.example`.
- Use a separate least-privilege database account for each service instead of the local MySQL root account.
- Keep Eureka, database and backend service ports on private networking. Expose only HTTPS frontend/gateway routes.
- Set `FRONTEND_ORIGIN` to the exact deployed frontend origin and `VITE_API_BASE_URL` to the deployed HTTPS gateway URL at frontend build time.
- Configure TLS/HTTPS, rate limiting and secure response headers at the reverse proxy/platform.
- Replace `DDL_AUTO=update` with a versioned migration tool and schema validation.
- Use a persistent volume or object storage for images.
- Add inventory/stock checks, checkout idempotency, audit logs, backup/restore tests and a real payment provider before accepting payments.
- Pass latest CI, then test registration → login → browse → cart → test checkout → order history manually.
