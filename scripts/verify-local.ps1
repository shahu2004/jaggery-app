param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$FrontendUrl = "http://localhost:5173"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

function Get-WithRetry {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [int]$Attempts = 30
    )
    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            return Invoke-RestMethod -Method Get -Uri $Uri -TimeoutSec 5
        } catch {
            if ($i -eq $Attempts) { throw }
            Start-Sleep -Seconds 2
        }
    }
}

Write-Host "Checking frontend..." -ForegroundColor Cyan
$front = Invoke-WebRequest -Method Get -Uri $FrontendUrl -TimeoutSec 10
if ($front.StatusCode -lt 200 -or $front.StatusCode -ge 300) {
    throw "Frontend returned HTTP $($front.StatusCode)"
}
Write-Host "PASS frontend is responding" -ForegroundColor Green

Write-Host "Waiting for gateway and product route..." -ForegroundColor Cyan
$products = Get-WithRetry -Uri "$BaseUrl/api/products?page=0&size=12"
if ($null -eq $products.content) {
    throw "Product endpoint response did not include a paginated content field."
}
Write-Host "PASS gateway and product API respond; $($products.totalElements) products found" -ForegroundColor Green

$email = "smoke-$([guid]::NewGuid().ToString('N').Substring(0, 10))@example.com"
$password = "Smoke!$([guid]::NewGuid().ToString('N').Substring(0, 16))"
$registration = @{ name = "Jaggery Smoke Test"; email = $email; password = $password } | ConvertTo-Json

Write-Host "Testing registration and profile..." -ForegroundColor Cyan
$created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registration -TimeoutSec 15
if ([string]::IsNullOrWhiteSpace($created.token)) { throw "Registration did not return a bearer token." }
if ($created.role -ne "CUSTOMER") { throw "Expected public registration to assign CUSTOMER, got '$($created.role)'." }
$headers = @{ Authorization = "Bearer $($created.token)" }
$profile = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/users/me" -Headers $headers -TimeoutSec 15
if ($profile.email -ne $email) { throw "Authenticated profile email did not match the registered email." }
Write-Host "PASS registration, JWT and authenticated profile" -ForegroundColor Green

if (@($products.content).Count -eq 0) {
    Write-Warning "No active products exist, so cart/checkout smoke checks were skipped. Add a product using an approved seller account and rerun the script."
    Write-Host "Core smoke checks passed." -ForegroundColor Green
    exit 0
}

$product = @($products.content)[0]
Write-Host "Testing cart and simulated checkout with product ID $($product.id)..." -ForegroundColor Cyan
$cartPayload = @{ productId = [long]$product.id; quantity = 1 } | ConvertTo-Json
$null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/cart/items" -Headers $headers -ContentType "application/json" -Body $cartPayload -TimeoutSec 15
$cart = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/cart" -Headers $headers -TimeoutSec 15
if (-not (@($cart | Where-Object { $_.productId -eq $product.id }).Count -gt 0)) { throw "Added product was not found in the cart." }

$order = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/orders/checkout" -Headers $headers -TimeoutSec 20
$orders = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/orders" -Headers $headers -TimeoutSec 15
if (-not (@($orders | Where-Object { $_.id -eq $order.id }).Count -gt 0)) { throw "Created order was not found in order history." }
Write-Host "PASS cart, simulated checkout and order history (order ID $($order.id))" -ForegroundColor Green
Write-Host "SMOKE TEST PASSED. No real payment was processed." -ForegroundColor Green
