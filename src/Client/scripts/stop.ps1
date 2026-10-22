$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pidFile = Join-Path $Root '.run\processes.json'
if (-not (Test-Path $pidFile)) { Write-Host 'No tracked SGU Client services found.'; exit 0 }
$rows = @(Get-Content $pidFile -Raw | ConvertFrom-Json)
foreach ($row in $rows) {
    foreach ($rawId in @($row.pid)) {
        $idToStop = 0
        if (-not [int]::TryParse([string]$rawId,[ref]$idToStop)) { continue }
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$idToStop" -ErrorAction SilentlyContinue
        if (-not $process) { continue }
        $expected = [string]$row.jar
        if ($process.Name -in @('java.exe','javaw.exe') -and $process.CommandLine -and $process.CommandLine.Contains($expected)) {
            Stop-Process -Id $idToStop -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped $($row.name) (PID $idToStop)"
        } else { Write-Host "Skipping PID $idToStop: it is not a tracked SGU Client process" }
    }
}
Remove-Item $pidFile -Force
