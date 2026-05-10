param(
    [string]$VaultAddr = "http://127.0.0.1:8200",
    [string]$VaultToken = "root",
    [string]$SecretPath = "secret/library",
    [switch]$RotateSecret
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-Base64Secret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Ensure-VaultRunning {
    param(
        [string]$Addr,
        [string]$Token
    )

    $env:VAULT_ADDR = $Addr
    $env:VAULT_TOKEN = $Token

    try {
        $null = vault status 2>$null
        Write-Host "Vault already running at $Addr"
        return
    } catch {
        Write-Host "Starting Vault dev server..."
        Start-Process -FilePath "vault" -ArgumentList 'server -dev -dev-root-token-id="root"' -WindowStyle Normal | Out-Null

        $maxAttempts = 30
        for ($i = 1; $i -le $maxAttempts; $i++) {
            Start-Sleep -Milliseconds 500
            try {
                $null = vault status 2>$null
                Write-Host "Vault is up."
                return
            } catch {
                # keep waiting
            }
        }

        throw "Vault did not become ready in time."
    }
}

function Ensure-VaultSecret {
    param(
        [string]$Path,
        [switch]$Rotate
    )

    $existingSecret = $null
    try {
        $json = vault kv get -format=json $Path 2>$null | ConvertFrom-Json
        if ($json -and $json.data -and $json.data.data) {
            $existingSecret = $json.data.data."app.jwt.secret"
        }
    } catch {
        # Secret path may not exist yet.
    }

    if ($Rotate -or [string]::IsNullOrWhiteSpace($existingSecret)) {
        $newSecret = New-Base64Secret
        vault kv put $Path "app.jwt.secret=$newSecret" | Out-Null
        Write-Host "Wrote app.jwt.secret to $Path"
    } else {
        Write-Host "app.jwt.secret already exists in $Path"
    }

    vault kv metadata get $Path | Out-Null
}

Write-Host "=== Dev bootstrap ==="
Write-Host "Vault addr: $VaultAddr"
Write-Host "Secret path: $SecretPath"

Ensure-VaultRunning -Addr $VaultAddr -Token $VaultToken
Ensure-VaultSecret -Path $SecretPath -Rotate:$RotateSecret

Write-Host "Starting Spring Boot with dev profile..."
$env:SPRING_PROFILES_ACTIVE = "dev"
./mvnw.cmd spring-boot:run