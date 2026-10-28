# Jaggery Marketplace Frontend

React + Vite frontend for the Jaggery App backend. It calls the Spring Cloud Gateway at `http://localhost:8080` by default.

## Run the full local stack

From the repository root, copy `.env.example` to `.env`, replace the local development secret values, then run:

```bash
docker compose up --build
```

Open http://localhost:5173.

## Run only the frontend

1. Install Node.js 22 or newer.
2. Start MySQL, Eureka, Auth Service, Product Service, Order Service and API Gateway as described in the root README.
3. From `frontend/`, run `npm install`, copy `.env.example` to `.env`, then run `npm run dev`.

## Features

- Product catalog, search, type/price filtering, sorting and pagination
- Customer registration/login and bearer-token session
- Cart, server-priced simulated checkout, order history and cancellation
- Seller product CRUD and image upload for approved seller/admin accounts
- Administrator category management

Checkout is a test flow only; it does not process payments. Product prices and permissions must remain validated by the backend.
