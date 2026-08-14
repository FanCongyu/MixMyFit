$ErrorActionPreference = "Stop"

function Find-Maven {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) {
        return $mvn.Source
    }

    $mvnCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mvnCmd) {
        return $mvnCmd.Source
    }

    $wrapperRoot = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
    if (Test-Path -LiteralPath $wrapperRoot) {
        $cached = Get-ChildItem -LiteralPath $wrapperRoot -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
        if ($cached) {
            return $cached
        }
    }

    throw "Maven was not found. Install Maven, add mvn to PATH, or run from an environment with Maven wrapper cache."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$maven = Find-Maven

Push-Location $backendDir
try {
    & $maven test "-Dtest=CoreFlowE2eTest"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
