$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $Root
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install JDK 21+ and add javac to PATH.' }
if (-not (Get-Command jar -ErrorAction SilentlyContinue)) { throw 'Install JDK 21+ and add jar to PATH.' }
$modules = @(
    @{ dir='app'; jar='Client.jar'; main='com.sgu.web.Application' },
    @{ dir='narration-server'; jar='narration-server.jar'; main='com.sgu.narration.Application' },
    @{ dir='audio-server'; jar='audio-server.jar'; main='com.sgu.audio.Application' }
)
# Do not try to overwrite a running JAR. Users should run STOP_CLIENT.bat first.
foreach ($module in $modules) {
    $dest = Join-Path (Join-Path $Root $module.dir) $module.jar
    $active = @(Get-CimInstance Win32_Process -Filter "name='java.exe' or name='javaw.exe'" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($dest) })
    if ($active.Count -gt 0) { throw "JAR is currently running: $dest. Run STOP_CLIENT.bat before building." }
}
foreach ($module in $modules) {
    $dir = Join-Path $Root $module.dir
    $target = Join-Path $dir 'target'
    $classes = Join-Path $target 'classes'
    if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    $sources = @(Get-ChildItem (Join-Path $dir 'src\main\java') -Recurse -Filter '*.java' -File | ForEach-Object { $_.FullName })
    $argsFile = Join-Path $target 'sources.txt'
    [IO.File]::WriteAllLines($argsFile, @($sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' }), (New-Object Text.UTF8Encoding($false)))
    & javac --release 21 -encoding UTF-8 -d $classes ('@' + $argsFile)
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed: $($module.dir)" }
    $resources = Join-Path $dir 'src\main\resources'
    if (Test-Path $resources) { Copy-Item (Join-Path $resources '*') $classes -Recurse -Force }
    $manifest = "Manifest-Version: 1.0`nMain-Class: $($module.main)`nClass-Path: ../lib/mysql-connector-j-9.7.0.jar`n`n"
    $manifestPath = Join-Path $target 'MANIFEST.MF'
    [IO.File]::WriteAllText($manifestPath,$manifest,(New-Object Text.UTF8Encoding($false)))
    $newJar = Join-Path $target $module.jar
    & jar --create --file $newJar --manifest $manifestPath -C $classes .
    if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed: $($module.dir)" }
    $dest = Join-Path $dir $module.jar
    try { Copy-Item $newJar $dest -Force } catch { throw "Cannot replace $dest. Stop old Java server(s) first: $_" }
    Write-Host "Built $($module.dir)" -ForegroundColor Green
}
Write-Host 'Build OK: Client + narration-server + audio-server' -ForegroundColor Green
