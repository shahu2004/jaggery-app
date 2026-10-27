# Jaggery Marketplace Frontend

React + Vite frontend for the Jaggery App backend. By default it calls the Spring Cloud Gateway at `http://localhost:8080`.

## Run locally

1. Install Node.js 22 or newer.
2. Start Eureka, Auth Service, Product Service, Order Service and API Gateway, and configure the required environment variables as documented in the repository root README.
3. From this directory:

```bash
npm install
cp .env.example .env
npm run dev
```

Open http://localhost:5173.

For production/static hosting, set `VITE_API_BASE_URL` to your deployed gateway URL and rebuild with `npm run build`.

## Current MVP screens

- Product catalog with search, type/price filtering and sorting
- Registration and login (new accounts are CUSTOMER until approved by an administrator)
- Authenticated cart, simulated checkout and order history/cancellation
- Seller product CRUD and image upload for approved SELLER/ADMIN accounts
- Administrator category creation

Checkout is a test flow only: this frontend does not collect or process payments. The backend remains the authority for product prices and permissions.
