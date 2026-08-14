$ErrorActionPreference = "Stop"

function Require-Env([string] $Name) {
    $Value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required environment variable: $Name"
    }
    return $Value.TrimEnd("/")
}

function Invoke-JsonPost([string] $Url, [hashtable] $Body) {
    $Json = $Body | ConvertTo-Json -Compress
    return Invoke-WebRequest -Uri $Url -Method Post -Body $Json -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
}

$WebUiUrl = Require-Env "MIXMYFIT_WEBUI_URL"
$ApiBaseUrl = Require-Env "MIXMYFIT_API_BASE_URL"
$HealthUrl = Require-Env "MIXMYFIT_HEALTH_URL"

$HealthResponse = Invoke-WebRequest -Uri $HealthUrl -Method Get -UseBasicParsing -TimeoutSec 30
if ($HealthResponse.StatusCode -ne 200) {
    throw "Backend health returned HTTP $($HealthResponse.StatusCode)"
}
$HealthBody = $HealthResponse.Content | ConvertFrom-Json
if ($HealthBody.status -ne "UP") {
    throw "Backend health status was '$($HealthBody.status)' instead of 'UP'"
}

$WebResponse = Invoke-WebRequest -Uri $WebUiUrl -Method Get -UseBasicParsing -TimeoutSec 30
if ($WebResponse.StatusCode -ne 200 -or $WebResponse.Content -notmatch 'id="app"') {
    throw "WebUI did not return the built Vue entry page"
}

$SmokeUsername = "smoke-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$SmokePassword = "Smoke123!"
$RegisterUrl = "$ApiBaseUrl/api/auth/register"
$LoginUrl = "$ApiBaseUrl/api/auth/login"

Invoke-JsonPost $RegisterUrl @{
    username = $SmokeUsername
    password = $SmokePassword
    confirmPassword = $SmokePassword
    nickname = "Railway Smoke"
} | Out-Null

$LoginResponse = Invoke-JsonPost $LoginUrl @{
    username = $SmokeUsername
    password = $SmokePassword
}

$SetCookie = $LoginResponse.Headers["Set-Cookie"] -join "; "
foreach ($Expected in @("MMF_SESSION", "HttpOnly", "Secure", "SameSite=None")) {
    if ($SetCookie -notmatch [regex]::Escape($Expected)) {
        throw "Login Set-Cookie header did not include $Expected"
    }
}

Write-Host "Vercel + Railway smoke check passed: health, WebUI, and MMF_SESSION cookie flags verified."
