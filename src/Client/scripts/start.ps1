param([ValidateSet('demo','live')][string]$Mode = 'demo')
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ProcessDir = Join-Path $Root '.run'
$LogDir = Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $ProcessDir,$LogDir | Out-Null
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { throw 'Java 21+ must be installed.' }
$version = (cmd /c 'java -version 2>&1' | Out-String)
Write-Host $version.Trim()
if ($version -match 'version "(?:1\.)?(\d+)' -and [int]$Matches[1] -lt 21) { throw 'JDK/JRE 21+ required.' }
$services = @(
    @{ name='narration-server'; port=8083; jar='narration-server\narration-server.jar' },
    @{ name='audio-server'; port=8084; jar='audio-server\audio-server.jar' },
    @{ name='Client'; port=8080; jar='app\Client.jar' }
)
foreach ($svc in $services) {
    $jar = Join-Path $Root $svc.jar
    if (-not (Test-Path $jar -PathType Leaf)) { throw "Missing $jar. Run BUILD_CLIENT.bat." }
    $listener = Get-NetTCPConnection -LocalPort $svc.port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) { throw "Port $($svc.port) is in use (PID $($listener.OwningProcess)). Stop old server before starting." }
}
$env:APP_MODE = $Mode
$rows = @()
try {
    foreach ($svc in $services) {
        $jar = Join-Path $Root $svc.jar
        $arguments = ('"-Dclient.home=' + $Root + '" -jar "' + $jar + '"')
        $p = Start-Process -FilePath $java.Source -ArgumentList $arguments -WorkingDirectory $Root -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $LogDir ($svc.name+'.log')) -RedirectStandardError (Join-Path $LogDir ($svc.name+'.error.log'))
        $rows += [pscustomobject]@{ name=$svc.name; pid=$p.Id; jar=$svc.jar }
        Write-Host "Started $($svc.name) (PID $($p.Id))"
    }
    ConvertTo-Json -InputObject @($rows) -Depth 4 | Set-Content (Join-Path $ProcessDir 'processes.json') -Encoding UTF8
    $ready = $false
    for ($i=0; $i -lt 50; $i++) {
        $good = $true
        foreach ($port in @(8080,8083,8084)) {
            try {
                $tcp = New-Object Net.Sockets.TcpClient
                $connection = $tcp.BeginConnect('127.0.0.1',$port,$null,$null)
                $ok = $connection.AsyncWaitHandle.WaitOne(250)
                if ($ok) { $tcp.EndConnect($connection) } else { $good = $false }
                $tcp.Close()
            } catch { $good = $false }
        }
        if ($good) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'Services did not become ready. Check logs/*.error.log.' }
    Write-Host 'READY - http://localhost:8080/' -ForegroundColor Green
    Start-Process 'http://localhost:8080/'
} catch {
    Write-Host "Startup error: $_" -ForegroundColor Red
    foreach ($r in $rows) { Stop-Process -Id ([int]$r.pid) -Force -ErrorAction SilentlyContinue }
    $errorLog = Join-Path $LogDir 'Client.error.log'
    if (Test-Path $errorLog) { Get-Content $errorLog -Tail 20 }
    exit 1
}
