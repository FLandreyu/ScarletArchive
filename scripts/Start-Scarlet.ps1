param([switch]$SkipBuild,[switch]$NoBrowser)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
foreach ($name in @('java','mvn','npm')) { if (-not (Get-Command $name -ErrorAction SilentlyContinue)) { throw "Install Java 21-25, Maven 3.6+ and Node.js 22.12+ first. Missing: $name" } }
$port=if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8098 }
$listener=Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
if ($listener) {
    try { $health=Invoke-RestMethod "http://127.0.0.1:$port/api/health" -TimeoutSec 3 } catch { $health=$null }
    if ($health.data.application -eq 'ScarletArchive') { Write-Host "Scarlet Archive is already running: http://127.0.0.1:$port"; if (-not $NoBrowser) { Start-Process "http://127.0.0.1:$port" }; exit 0 }
    throw "Port $port belongs to another service. Set SERVER_PORT to another port."
}
if (-not $SkipBuild) {
    Push-Location -LiteralPath (Join-Path $projectRoot 'frontend')
    try {
        if (-not (Test-Path -LiteralPath node_modules)) { npm.cmd ci --no-fund --no-audit; if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed' } }
        npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    } finally { Pop-Location }
    Push-Location -LiteralPath (Join-Path $projectRoot 'backend')
    try { mvn.cmd -B -DskipTests package; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' } } finally { Pop-Location }
}
$jar=Join-Path $projectRoot 'backend\target\scarlet-archive-1.0.0.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Run without -SkipBuild first.' }
Write-Host "Scarlet Archive: http://127.0.0.1:$port  (Ctrl+C to stop)" -ForegroundColor Magenta
Write-Host 'Local admin: admin / Scarlet123!'
if (-not $NoBrowser) {
    $openScript=@'
$url='http://127.0.0.1:PORT_PLACEHOLDER'
for($n=0;$n -lt 60;$n++){try{$r=Invoke-RestMethod "$url/api/health" -TimeoutSec 2;if($r.data.application -eq 'ScarletArchive'){Start-Sleep -Seconds 2;Start-Process $url;exit}}catch{};Start-Sleep -Seconds 1}
'@
    $encoded=[Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($openScript.Replace('PORT_PLACEHOLDER',[string]$port)))
    Start-Process -FilePath powershell.exe -ArgumentList @('-NoProfile','-EncodedCommand',$encoded) -WindowStyle Hidden
}
Set-Location -LiteralPath (Join-Path $projectRoot 'backend')
java '-Dfile.encoding=UTF-8' -jar $jar '--debug=false'
exit $LASTEXITCODE
