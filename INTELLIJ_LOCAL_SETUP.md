# Run Jaggery locally in IntelliJ IDEA (without Docker)

This guide uses the existing microservices. It does not replace the project or require Docker. The verified source branch is `feature/mvp-completion-roadmap`; see the repository branch and CI checks before changing dependencies.

## 1. Use the right project and JDK

1. In IntelliJ IDEA, choose **File → Open** and open the repository root folder `jaggery-app`.
2. Set **File → Project Structure → Project SDK** to **JDK 21** and Project language level to 21.
3. Open **Settings → Build, Execution, Deployment → Build Tools → Maven**. Set Maven home to **Use Maven wrapper** and importer/runner JDK to **Project JDK (21)**.
4. The backend is five separate Maven projects, not one root Maven aggregator. Add these POMs to the Maven tool window (click **+ / Add Maven Project**):
   - `backend/discovery-server/discovery-server/pom.xml`
   - `backend/auth-service/auth-service/pom.xml`
   - `backend/product-service/product-service/pom.xml`
   - `backend/order-service/order-service/pom.xml`
   - `backend/api-gateway/api-gateway/pom.xml`

Do not run Maven from the repository root; there is no root `pom.xml`.

## 2. Start local MySQL and create the databases

Install/start a local MySQL 8.x service. Keep it listening on `localhost:3306`.

Create the three empty databases using MySQL Workbench, or from PowerShell where the MySQL CLI is on PATH (PowerShell does not support Bash-style `<` input redirection):

```powershell
Get-Content .\\scripts\\create-local-databases.sql -Raw | mysql -u root -p
```

The script creates:
- `jaggery_auth`
- `jaggery_product`
- `jaggery_order`

Use the same MySQL username/password for all three services initially. The examples below assume username `root`; substitute the password you actually configured for local MySQL.

## 3. Create five IntelliJ Maven run configurations

Use **Run → Edit Configurations → + → Maven**. Create one configuration per service. For each configuration, set its **Working directory** to the module folder and **Command line** to:

```
spring-boot:run
```

Set **Environment variables** as shown below. Keep secrets in the local run configuration, not in Git.

| Configuration | Working directory | Environment variables |
|---|---|---|
| Discovery Server | `$PROJECT_DIR$/backend/discovery-server/discovery-server` | No variables required |
| Auth Service | `$PROJECT_DIR$/backend/auth-service/auth-service` | `DB_USERNAME=root;DB_PASSWORD=YOUR_MYSQL_PASSWORD;JWT_SECRET=YOUR_LOCAL_SECRET_AT_LEAST_32_BYTES` |
| Product Service | `$PROJECT_DIR$/backend/product-service/product-service` | `DB_USERNAME=root;DB_PASSWORD=YOUR_MYSQL_PASSWORD;JWT_SECRET=THE_SAME_LOCAL_SECRET;UPLOAD_DIR=./uploads` |
| Order Service | `$PROJECT_DIR$/backend/order-service/order-service` | `DB_USERNAME=root;DB_PASSWORD=YOUR_MYSQL_PASSWORD;JWT_SECRET=THE_SAME_LOCAL_SECRET;PRODUCT_SERVICE_URL=http://localhost:8082` |
| API Gateway | `$PROJECT_DIR$/backend/api-gateway/api-gateway` | `FRONTEND_ORIGIN=http://localhost:5173;EUREKA_URL=http://localhost:8761/eureka` |

Replace the example values before starting. Use the **same JWT_SECRET** for Auth, Product and Order Services. It must be at least 32 bytes. Do not reuse the example value in a deployed application.

If your IntelliJ Maven configuration does not show an Environment variables field, use **Edit Configurations → Modify options → Environment variables**, or run the module's `mvnw.cmd spring-boot:run` from a PowerShell terminal after setting the variables in that terminal.

The services already provide these default local ports and database URLs:
- Discovery Server: `8761`
- Auth Service: `8081` → `jaggery_auth`
- Product Service: `8082` → `jaggery_product`
- Order Service: `8083` → `jaggery_order`
- API Gateway: `8080`

The datasource username defaults to `root`, and each datasource URL defaults to its local database. `DB_PASSWORD` and `JWT_SECRET` are deliberately required.

## 4. Start the backend in this order

Start each run configuration and leave it running:

1. **Discovery Server** — wait for the application to start on port 8761.
2. **Auth Service** — connects to MySQL and registers with Eureka.
3. **Product Service** — connects to MySQL, creates the local `uploads` directory, and registers with Eureka.
4. **Order Service** — connects to MySQL and calls Product Service on port 8082.
5. **API Gateway** — registers with Eureka and routes browser requests to the services.

Open the Eureka dashboard at [http://localhost:8761](http://localhost:8761). Wait for `AUTH-SERVICE`, `PRODUCT-SERVICE`, `ORDER-SERVICE`, and `API-GATEWAY` to register.

## 5. Run React separately in VS Code

Open the `frontend` folder in VS Code. In its terminal:

```powershell
npm install
npm test
npm run dev
```

The Vite configuration already proxies `/api/**` to `http://localhost:8080`. You do not need a public API URL for local development. Open [http://localhost:5173](http://localhost:5173).

## 6. Verify the services

Run these checks in PowerShell after all services have started:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8083/actuator/health
Invoke-RestMethod http://localhost:8080/api/auth/ping
Invoke-RestMethod 'http://localhost:8080/api/products?page=0&size=12'
```

The auth ping should report `service=auth-service` and `status=running`. The products request should return a page object with a `content` array (which may initially be empty). The frontend's status panel should say the services are connected.

For an end-to-end smoke test after adding at least one product, run from the repository root:

```powershell
./scripts/verify-local.ps1
```

That script registers a temporary customer, verifies the JWT/profile, and checks cart/checkout/order history if at least one active product exists. Checkout is simulated; no real payment is processed.

## 7. Optional Maven build checks

In each module's IntelliJ terminal, run:

```powershell
.\mvnw.cmd -U clean verify
```

For Auth, Product, and Order tests, local MySQL must be running, the three databases must exist, and the required `DB_PASSWORD` and `JWT_SECRET` environment variables must be set in that terminal. The repository's GitHub Actions CI has already run these module tests successfully on the audited branch commit.

## 8. Common errors

| Error | Check |
|---|---|
| Could not resolve placeholder `DB_PASSWORD` or `JWT_SECRET` | Add both variables to that service's IntelliJ run configuration; check spelling/case. |
| `Unknown database 'jaggery_auth'` (or product/order) | Run `scripts/create-local-databases.sql` in local MySQL. |
| `Access denied for user 'root'` | Set `DB_USERNAME` and `DB_PASSWORD` to the credentials that work in MySQL Workbench. |
| `Communications link failure` | Start the local MySQL Windows service and verify port 3306. |
| `JWT_SECRET must be at least 32...` | Use the same secret of at least 32 bytes in Auth, Product and Order. |
| Eureka shows no services | Start Discovery Server first, then services; verify their `EUREKA_URL` is `http://localhost:8761/eureka`. |
| Gateway returns 503 | Check that the target service is running and registered in Eureka. |
| Frontend cannot reach API | Start Gateway on 8080 and Vite on 5173; keep the Vite `/api` proxy enabled. |
| `Address already in use` | Find the process using that port with `Get-NetTCPConnection -LocalPort 8080 -State Listen` (change the port as needed), then stop only the process you recognize. |

## 9. Lombok and IntelliJ indexing

The Maven POMs include Lombok and its annotation processor. If IntelliJ marks generated getters/builders as unresolved while Maven builds successfully, install/enable the Lombok plugin if available in your IDEA edition and enable **Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing**. Reimport all Maven projects.

## 10. Important safety note

These steps are for local development only. Never commit passwords or JWT secrets, never use the local secret in a hosted deployment, do not expose MySQL/Eureka publicly, and do not treat the test checkout as real payment processing.
