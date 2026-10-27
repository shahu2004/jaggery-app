# Deployment options: Render and Railway

This is a manual deployment guide. The repository includes Dockerfiles and a local Docker Compose stack; it does not deploy itself to a cloud account. Choose one host for the application services and configure the database credentials there.

## 1. Recommended first deployment: validate locally

Use the [local Docker Compose guide](../DEPLOYMENT.md) first. The latest completed checks are in [GitHub Actions](https://github.com/shahu2004/jaggery-app/actions). A successful CI build does not prove that external hosting, database connectivity, TLS or an end-to-end browser flow is correctly configured.

## 2. Hosting notes

The Spring services now use `server.port: ${PORT:defaultPort}`, so they keep their existing local ports but can bind to a hosting platform's assigned `PORT`. Render requires web services to bind to the configured port (default 10000) and recommends using `PORT` ([Render web-service docs](https://render.com/docs/web-services)).

Railway provides MySQL environment variables such as `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE` and `MYSQL_URL`; see the [Railway MySQL docs](https://docs.railway.com/databases/mysql). When all services run in one Railway project, prefer private networking for service-to-service and database connections.

## 3. Cloud deployment checklist

Deploy the components in this order:
1. MySQL database. Create the three databases `jaggery_auth`, `jaggery_product`, and `jaggery_order` (the init SQL file in this repository lists the schemas).
2. Eureka Discovery Server.
3. Auth Service.
4. Product Service.
5. Order Service.
6. API Gateway.
7. React frontend.

For each Spring service, choose its own Dockerfile and Docker context:

| Service | Dockerfile path | Required configuration |
|---|---|---|
| Eureka | `backend/discovery-server/discovery-server/Dockerfile` | `PORT` (default 8761) |
| Auth | `backend/auth-service/auth-service/Dockerfile` | `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `EUREKA_URL` |
| Product | `backend/product-service/product-service/Dockerfile` | `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, same `JWT_SECRET`, `EUREKA_URL`, `UPLOAD_DIR` |
| Order | `backend/order-service/order-service/Dockerfile` | `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, same `JWT_SECRET`, `EUREKA_URL`, `PRODUCT_SERVICE_URL` |
| Gateway | `backend/api-gateway/api-gateway/Dockerfile` | `PORT`, `EUREKA_URL`, `FRONTEND_ORIGIN` |
| Frontend | `frontend/Dockerfile` | build arg `VITE_API_BASE_URL` set to the gateway's public HTTPS URL |

### Important environment details

- **JWT:** generate one random secret of at least 32 bytes and supply the exact same `JWT_SECRET` to Auth, Product and Order Service. Never commit it to Git.
- **Eureka:** set `EUREKA_URL` in every client and in the gateway to the discovery server's private/internal address, including its actual port. Do not use `localhost` between separate cloud services.
- **Product Service:** set `PRODUCT_SERVICE_URL` in Order Service to Product Service's private/internal URL including port. Do not use `localhost` between separate containers/services.
- **MySQL:** set a complete JDBC `DB_URL` separately for each service, pointing to its schema. Do not assume Railway's URL is already a JDBC URL. If the app and database are on different providers, use the database provider's approved external connection endpoint and protect it carefully.
- **Frontend:** set `VITE_API_BASE_URL` at build time to the public HTTPS gateway URL. Set gateway `FRONTEND_ORIGIN` to the frontend's exact origin (no trailing path).
- **Image uploads:** local container files are not enough for production. Attach persistent disk/object storage and configure `UPLOAD_DIR`, or modify the image service to use object storage.
- **Network:** expose only gateway/frontend publicly; keep database, Eureka and internal service ports private.
- **Health checks:** use `/actuator/health` for Spring services if the hosting platform supports HTTP health checks. Do not expose management endpoints beyond the required health/info endpoints.
- **Database:** replace `DDL_AUTO=update` with Flyway/Liquibase migrations before production, and use a least-privilege database user rather than MySQL root.
- **Checkout:** this is a test/simulated checkout. Do not accept real payments until payment, idempotency, inventory consistency, order audit and refund/cancellation behavior are implemented and tested.

## 4. Required smoke test after deployment

Run the following from the browser/Postman using the deployed gateway:
1. `GET /api/products?page=0&size=12` returns a page (empty catalog is acceptable).
2. Register a new account and log in; the response should return a bearer token and the role should be `CUSTOMER`.
3. With that token, `GET /api/users/me` returns the signed-in profile.
4. Add a product to the cart using `POST /api/cart/items`, then verify `GET /api/cart`.
5. Place a test order with `POST /api/orders/checkout` and verify `GET /api/orders`.
6. Confirm unauthenticated order writes fail and one customer cannot read another customer's order.
7. Separately bootstrap a local/test admin following [LOCAL_ADMIN_BOOTSTRAP.md](LOCAL_ADMIN_BOOTSTRAP.md), sign in again, approve a customer as seller, and verify the seller can only manage their own products.

Do not mark deployment complete until these checks pass against the actual hosted endpoints.
