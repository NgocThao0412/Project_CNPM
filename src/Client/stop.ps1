$ErrorActionPreference='Stop'
$Root=$PSScriptRoot
$pidFile=Join-Path $Root '.run\processes.json'
if (-not (Test-Path $pidFile)) { Write-Host 'No processes recorded by this bundle.'; exit }
$rows=@(Get-Content $pidFile -Raw | ConvertFrom-Json)
foreach ($row in $rows) {
    $idToStop=[int]$row.pid
    $process=Get-CimInstance Win32_Process -Filter "ProcessId=$idToStop" -ErrorAction SilentlyContinue
    if ($process -and $process.CommandLine -and $process.CommandLine.Contains([string]$row.jar)) {
        Stop-Process -Id $idToStop -ErrorAction SilentlyContinue
        Write-Host "Stopped $($row.name) ($idToStop)"
    }
}
Remove-Item $pidFile -Force
