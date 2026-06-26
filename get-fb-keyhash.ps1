# Facebook Key Hash Generator for Windows

$keystorePath = "$env:USERPROFILE\.android\debug.keystore"
$alias = "androiddebugkey"
$storepass = "android"
$keypass = "android"

Write-Host "Generating Facebook Key Hash..." -ForegroundColor Green

# Export certificate
$certBytes = & keytool -exportcert -alias $alias -keystore $keystorePath -storepass $storepass -keypass $keypass 2>&1

# Check if keytool command succeeded
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Could not export certificate" -ForegroundColor Red
    exit 1
}

# Calculate SHA1 hash and encode to Base64
try {
    $sha1 = New-Object System.Security.Cryptography.SHA1CryptoServiceProvider
    
    # Export cert to a temp file (binary DER format)
    $tempFile = [System.IO.Path]::GetTempFileName()
    & keytool -exportcert -alias $alias -keystore $keystorePath -storepass $storepass -keypass $keypass -file $tempFile 2>$null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: keytool failed. Check your Java installation." -ForegroundColor Red
        Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
        exit 1
    }
    
    # Read raw bytes and compute SHA1
    $certBytes = [System.IO.File]::ReadAllBytes($tempFile)
    $hashBytes = $sha1.ComputeHash($certBytes)
    $keyHash = [System.Convert]::ToBase64String($hashBytes)
    
    Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
    
    Write-Host "`nYour Facebook Key Hash:" -ForegroundColor Cyan
    Write-Host $keyHash -ForegroundColor Yellow
    Write-Host "`nCopy this key hash and add it to your Facebook App Dashboard" -ForegroundColor Green
    
} catch {
    Write-Host "Error generating hash: $_" -ForegroundColor Red
}
