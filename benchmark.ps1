param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("baseline", "after")]
    [string]$Phase,

    [Parameter(Mandatory = $true)]
    [string]$Url,

    [Parameter(Mandatory = $true)]
    [string]$Endpoint,

    [int]$Requests = 300,
    [int]$Warmup = 20,

# Use either -Token OR login credentials.
    [string]$Token,
    [string]$LoginEndpoint = "/api/auth/login",
    [string]$Username = "libraryuser",
    [string]$Password = "librarypass",

    [string]$BaselineFile = ".benchmark-results.json",

    [switch]$AllowAnonymous,
    [switch]$InsecureSkipTls
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-Percentile {
    param(
        [double[]]$Values,
        [double]$Percent
    )

    if ($Values.Count -eq 0) {
        return 0
    }

    $sorted = $Values | Sort-Object
    $index = [int][Math]::Floor(($Percent / 100.0) * ($sorted.Count - 1))
    return [double]$sorted[$index]
}

function New-AuthHeader {
    param(
        [string]$BaseUrl,
        [string]$GivenToken,
        [switch]$Anonymous
    )

    if ($Anonymous) {
        return @{}
    }

    if (-not [string]::IsNullOrWhiteSpace($GivenToken)) {
        return @{ Authorization = "Bearer $GivenToken" }
    }

    $loginUri = "$BaseUrl$LoginEndpoint"
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Method Post -Uri $loginUri -ContentType "application/json" -Body $body

    if (-not $loginResponse.accessToken) {
        throw "Login response did not contain accessToken."
    }

    return @{ Authorization = "Bearer $($loginResponse.accessToken)" }
}

function Get-StoreValue {
    param(
        [object]$Store,
        [string]$Key
    )

    if ($null -eq $Store) { return $null }

    if ($Store -is [System.Collections.IDictionary]) {
        if ($Store.Contains($Key)) { return $Store[$Key] }
        return $null
    }

    $prop = $Store.PSObject.Properties[$Key]
    if ($null -ne $prop) { return $prop.Value }
    return $null
}

if ($InsecureSkipTls) {
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
}

$baseUrl = $Url.TrimEnd('/')
$path = if ($Endpoint.StartsWith('/')) { $Endpoint } else { "/$Endpoint" }
$targetUri = "$baseUrl$path"

if ($Requests -lt 1) { throw "Requests must be >= 1." }
if ($Warmup -lt 0) { throw "Warmup must be >= 0." }

$headers = New-AuthHeader -BaseUrl $baseUrl -GivenToken $Token -Anonymous:$AllowAnonymous

Write-Host "Target: $targetUri"
Write-Host "Phase: $Phase"
Write-Host "Warmup: $Warmup, Requests: $Requests"

# Warmup
for ($i = 1; $i -le $Warmup; $i++) {
    Invoke-WebRequest -Method Get -Uri $targetUri -Headers $headers | Out-Null
}

# Measurement
$times = New-Object System.Collections.Generic.List[double]
for ($i = 1; $i -le $Requests; $i++) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-WebRequest -Method Get -Uri $targetUri -Headers $headers | Out-Null
    $sw.Stop()
    $times.Add($sw.Elapsed.TotalMilliseconds)
}

$avg = ($times | Measure-Object -Average).Average
$min = ($times | Measure-Object -Minimum).Minimum
$max = ($times | Measure-Object -Maximum).Maximum
$p95 = Get-Percentile -Values $times.ToArray() -Percent 95

$current = [ordered]@{
    phase = $Phase
    target = $targetUri
    requests = $Requests
    warmup = $Warmup
    avgMs = [Math]::Round($avg, 2)
    p95Ms = [Math]::Round($p95, 2)
    minMs = [Math]::Round($min, 2)
    maxMs = [Math]::Round($max, 2)
    timestamp = (Get-Date).ToString("s")
}

# Load existing store (robust for both object/hashtable JSON)
$store = [ordered]@{
    baseline = $null
    after = $null
    history = @()
    lastUpdated = $null
}

if (Test-Path $BaselineFile) {
    try {
        $loaded = Get-Content -Path $BaselineFile -Raw | ConvertFrom-Json
        if ($null -ne $loaded) {
            $store.baseline = Get-StoreValue -Store $loaded -Key "baseline"
            $store.after = Get-StoreValue -Store $loaded -Key "after"
            $loadedHistory = Get-StoreValue -Store $loaded -Key "history"
            if ($null -ne $loadedHistory) {
                $store.history = @($loadedHistory)
            }
            $store.lastUpdated = Get-StoreValue -Store $loaded -Key "lastUpdated"
        }
    } catch {
        Write-Warning "Could not parse '$BaselineFile'. Starting with a fresh store."
    }
}

if ($Phase -eq "baseline") {
    $store.baseline = $current
    $store.lastUpdated = (Get-Date).ToString("s")
    $store.history += [ordered]@{ phase = "baseline"; result = $current }

    $store | ConvertTo-Json -Depth 10 | Set-Content -Path $BaselineFile -Encoding UTF8
    Write-Host "BASELINE saved to $BaselineFile"
    Write-Host ("RESULT: avg={0}ms p95={1}ms min={2}ms max={3}ms" -f $current.avgMs, $current.p95Ms, $current.minMs, $current.maxMs)
    exit 0
}

$baseline = $store.baseline
if ($null -eq $baseline) {
    throw "Baseline missing in '$BaselineFile'. Run with -Phase baseline first."
}

if ([double]$baseline.avgMs -le 0 -or [double]$baseline.p95Ms -le 0) {
    throw "Baseline data is invalid: avgMs/p95Ms must be > 0."
}

$avgImprovement = (([double]$baseline.avgMs - [double]$current.avgMs) / [double]$baseline.avgMs) * 100.0
$p95Improvement = (([double]$baseline.p95Ms - [double]$current.p95Ms) / [double]$baseline.p95Ms) * 100.0

$afterResult = [ordered]@{
    measurement = $current
    avgImprovementPercent = [Math]::Round($avgImprovement, 2)
    p95ImprovementPercent = [Math]::Round($p95Improvement, 2)
}

$store.after = $afterResult
$store.lastUpdated = (Get-Date).ToString("s")
$store.history += [ordered]@{ phase = "after"; result = $afterResult }

$store | ConvertTo-Json -Depth 10 | Set-Content -Path $BaselineFile -Encoding UTF8

$summary = "RESULT: avg_before={0}ms avg_after={1}ms avg_improvement={2}% | p95_before={3}ms p95_after={4}ms p95_improvement={5}%" -f @(
    $baseline.avgMs,
    $current.avgMs,
    [Math]::Round($avgImprovement, 2),
    $baseline.p95Ms,
    $current.p95Ms,
    [Math]::Round($p95Improvement, 2)
)

Write-Host $summary
