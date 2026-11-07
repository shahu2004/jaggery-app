# Jaggery Marketplace Frontend

React 19 + Vite frontend for the Jaggery marketplace. The UI uses the existing Spring Cloud Gateway API and does not simulate successful sign-in, cart writes, or orders when the backend is offline.

## Recommended: run the frontend with IntelliJ-hosted backend services

Start local MySQL, then start Discovery Server, Auth Service, Product Service, Order Service and API Gateway in IntelliJ IDEA. Follow [INTELLIJ_LOCAL_SETUP.md](../INTELLIJ_LOCAL_SETUP.md) for the exact run configurations and required environment variables.

From this `frontend` directory in VS Code or PowerShell:

```powershell
npm install
npm test
npm run dev
```

Open http://localhost:5173. Vite proxies same-origin `/api/**` requests to `http://localhost:8080`; keep API Gateway running. You do not need Docker or a public API URL for this setup.

## Optional: run the complete application with Docker

From the repository root, copy `.env.example` to `.env`, set a local MySQL root password and a random JWT secret of at least 32 bytes, and run:

```bash
docker compose config
docker compose up --build
```

Open http://localhost:5173. The frontend Nginx server proxies `/api/**` to the API Gateway on the Docker network.

## Run the frontend separately

Prerequisites: Node.js 22+ and the complete backend stack already running, with the API Gateway reachable at port 8080.

From this directory:

```bash
cp .env.example .env
npm install
npm test
npm run dev
```

Vite listens on http://localhost:5173 and proxies `/api/**` to `VITE_DEV_API_TARGET` (default: `http://localhost:8080`). If your local API Gateway uses a different address, update that variable in `.env`.

## Backend connection status

The site checks the public auth ping endpoint at `GET /api/auth/ping`. If the gateway or Auth Service is unavailable, the page displays a connection panel with the setup command and a Retry button. Product data and account actions remain connected to the real backend; the UI does not silently fall back to fake success data.

## Deploy the frontend

### Docker/Nginx frontend

The container proxies `/api/**` through the Nginx server to `API_GATEWAY_URL`. Set that runtime variable to a URL reachable from the frontend container, for example:

```text
API_GATEWAY_URL=https://your-public-api-gateway.example.com
```

For services on one private Docker/Railway network, use the API Gateway's private service hostname and port instead. Keep database, Eureka and internal microservice ports private.

### Static frontend hosting

Static hosts do not run the included Nginx proxy. Set `VITE_API_BASE_URL` to the public HTTPS API Gateway URL at build time, and configure the gateway's `FRONTEND_ORIGIN` to the exact deployed frontend origin.

Never set `VITE_API_BASE_URL=http://localhost:8080` for a public website: in a visitor's browser, localhost refers to that visitor's own computer.

## Supported frontend flows

- Product catalog with search, product type/category/price filters, sorting and pagination
- Customer registration and login with bearer-token session storage
- Cart loading, quantity updates and item removal
- Simulated checkout using server-side product prices (no payment is collected)
- Customer order history and cancellation while order status is `PLACED`
- Seller product listing, create/update, soft-delete and image upload for approved seller/admin accounts
- Admin category management, seller approval and order status transitions
- Backend connection status and retry guidance

## Useful checks

```bash
npm test
npm run build
```

For the full backend smoke test, run the PowerShell script from the repository root:

```powershell
./scripts/verify-local.ps1
```

Checkout remains a test flow only. Do not represent it as real payment processing.
