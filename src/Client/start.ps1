param(
    [ValidateSet('demo','live')][string]$Mode='demo'
)
$ErrorActionPreference='Stop'
$Root=$PSScriptRoot
$processDir=Join-Path $Root '.run'
$logDir=Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $processDir,$logDir | Out-Null
$java=Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { Write-Host 'ERROR: Java is not installed or is missing from PATH.' -ForegroundColor Red; exit 1 }
$versionText=(cmd /c 'java -version 2>&1' | Out-String)
Write-Host $versionText.Trim()
if ($versionText -notmatch 'version "(?:1\.)?(\d+)') { Write-Host 'WARNING: Cannot verify Java version.' -ForegroundColor Yellow }
elseif ([int]$Matches[1] -lt 21) { Write-Host 'ERROR: Java 21 or newer is required.' -ForegroundColor Red; exit 1 }
$services=@(
    @{name='audio-server'; port=8084; jar='audio-server\audio-server.jar'},
    @{name='narration-server'; port=8083; jar='narration-server\narration-server.jar'},
    @{name='client-gateway'; port=8085; jar='client-gateway\client-gateway.jar'},
    @{name='client-server'; port=8080; jar='client-server\client-server.jar'}
)
foreach ($service in $services) {
    $jarPath=Join-Path $Root $service.jar
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        Write-Host "ERROR: Missing JAR $jarPath" -ForegroundColor Red
        exit 1
    }
}
# Do not kill any existing programs occupying the service ports.
foreach($service in $services) {
    $listener=Get-NetTCPConnection -LocalPort $service.port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if($listener) { Write-Host "ERROR: Port $($service.port) is already in use. Close the other application first." -ForegroundColor Red; exit 1 }
}
$env:APP_MODE=$Mode
$rows=@()
foreach($service in $services) {
    $logOut=Join-Path $logDir ($service.name+'.log')
    $logErr=Join-Path $logDir ($service.name+'.error.log')
    $arguments=@(('"-Dclient.home=' + $Root + '"'),'-jar', ('"' + (Join-Path $Root $service.jar) + '"'))
    try {
        $process=Start-Process -FilePath $java.Source -ArgumentList $arguments -WorkingDirectory $Root -PassThru -WindowStyle Hidden -RedirectStandardOutput $logOut -RedirectStandardError $logErr
    } catch {
        Write-Host "ERROR starting $($service.name): $_" -ForegroundColor Red
        foreach($row in $rows) { Stop-Process -Id $row.pid -ErrorAction SilentlyContinue }
        exit 1
    }
    $rows+=@{ name=$service.name; pid=$process.Id; jar=$service.jar }
    Write-Host "Started $($service.name) pid=$($process.Id)"
}
$rows | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 (Join-Path $processDir 'processes.json')
$ready=$false
for($i=0;$i -lt 40;$i++) {
    try {
        $tcp=New-Object System.Net.Sockets.TcpClient
        $async=$tcp.BeginConnect('127.0.0.1',8080,$null,$null)
        $connected=$async.AsyncWaitHandle.WaitOne(500)
        if($connected) { $tcp.EndConnect($async); $ready=$true }
        $tcp.Close()
        if($ready){ break }
    } catch { }
    Start-Sleep -Milliseconds 500
}
if(-not $ready) {
    Write-Host 'ERROR: Client did not start on port 8080. Check logs/client-server.error.log' -ForegroundColor Red
    foreach($service in $services) {
        $errorLog=Join-Path $logDir ($service.name+'.error.log')
        if((Test-Path $errorLog) -and (Get-Item $errorLog).Length -gt 0) {
            Write-Host "--- $($service.name) ---"
            Get-Content $errorLog -Tail 20
        }
    }
    exit 1
}
Write-Host 'READY: http://localhost:8080/' -ForegroundColor Green
Write-Host 'Press Enter to close this launcher; services stay running. Use stop.bat to stop them.'
Start-Process 'http://localhost:8080/'
