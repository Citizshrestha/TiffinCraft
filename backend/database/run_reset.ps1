# ============================================================================
# Reset Test Data - Execution Script
# ============================================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TiffinCraft Test Data Reset" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Load environment variables from .env file
$envPath = Join-Path $PSScriptRoot "..\..\.env"
if (Test-Path $envPath) {
    Write-Host "Loading database configuration from .env..." -ForegroundColor Yellow
    Get-Content $envPath | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "env:$key" -Value $value
        }
    }
} else {
    Write-Host "Warning: .env file not found at $envPath" -ForegroundColor Red
    Write-Host "Using default values..." -ForegroundColor Yellow
}

# Database configuration
$DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
$DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "root" }
$DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "" }
$DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "tiffincraft" }

Write-Host "Database Configuration:" -ForegroundColor Green
Write-Host "  Host: $DB_HOST" -ForegroundColor White
Write-Host "  Port: $DB_PORT" -ForegroundColor White
Write-Host "  User: $DB_USER" -ForegroundColor White
Write-Host "  Database: $DB_NAME" -ForegroundColor White
Write-Host ""

# Confirmation prompt
Write-Host "WARNING: This will DELETE all subscription, order, payment, and earnings data!" -ForegroundColor Red
Write-Host "The following will be reset:" -ForegroundColor Yellow
Write-Host "  - All subscriptions and subscription plans" -ForegroundColor White
Write-Host "  - All orders and payments" -ForegroundColor White
Write-Host "  - All commission settlements (earnings)" -ForegroundColor White
Write-Host "  - All reviews, favorites, and notifications" -ForegroundColor White
Write-Host "  - All chat conversations" -ForegroundColor White
Write-Host "  - All cart data" -ForegroundColor White
Write-Host ""
Write-Host "The following will be PRESERVED:" -ForegroundColor Green
Write-Host "  - Users (customers, cooks, admins)" -ForegroundColor White
Write-Host "  - Cook profiles" -ForegroundColor White
Write-Host "  - Meals" -ForegroundColor White
Write-Host ""

$confirmation = Read-Host "Are you sure you want to continue? (yes/no)"

if ($confirmation -ne "yes") {
    Write-Host ""
    Write-Host "Reset cancelled." -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "Starting reset..." -ForegroundColor Cyan

# SQL file path
$sqlFile = Join-Path $PSScriptRoot "reset_test_data.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Host "Error: SQL file not found at $sqlFile" -ForegroundColor Red
    exit 1
}

# Build MySQL command
$mysqlCmd = "mysql"
$mysqlArgs = @(
    "-h", $DB_HOST,
    "-P", $DB_PORT,
    "-u", $DB_USER
)

if ($DB_PASSWORD) {
    $mysqlArgs += "-p$DB_PASSWORD"
}

$mysqlArgs += @($DB_NAME, "<", $sqlFile)

Write-Host "Executing reset script..." -ForegroundColor Yellow

try {
    # Execute the SQL script
    $command = "$mysqlCmd $($mysqlArgs -join ' ')"
    Invoke-Expression $command
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  Reset Completed Successfully!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "✓ All test data has been cleared" -ForegroundColor Green
        Write-Host "✓ Users, cook profiles, and meals preserved" -ForegroundColor Green
        Write-Host "✓ System ready for fresh testing" -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "Error: Reset script execution failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "Error executing reset script: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Ensure MySQL is installed and in your PATH" -ForegroundColor White
    Write-Host "  2. Verify database credentials in backend/.env" -ForegroundColor White
    Write-Host "  3. Check if MySQL service is running" -ForegroundColor White
    Write-Host ""
    Write-Host "Manual execution command:" -ForegroundColor Yellow
    Write-Host "  mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME < `"$sqlFile`"" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}
