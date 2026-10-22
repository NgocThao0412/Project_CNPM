$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
Set-Location $Root
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 and add its bin folder to PATH. javac is required to rebuild.' }
$modules = @('client-core','client-gateway','narration-server','audio-server','client-server')
foreach ($module in $modules) {
    $moduleDir = Join-Path $Root $module
    $classes = Join-Path $moduleDir 'target\classes'
    if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $sources = @(Get-ChildItem (Join-Path $moduleDir 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    $sourceList = Join-Path $moduleDir 'target\sources.txt'
    $sourceLines = @($sources | ForEach-Object { '"' + $_.Replace('\','/') + '"' })
    [IO.File]::WriteAllLines($sourceList, $sourceLines, (New-Object Text.UTF8Encoding($false)))
    $compile = @('--release','21','-encoding','UTF-8','-d',$classes)
    if ($module -ne 'client-core') { $compile += @('-cp',(Join-Path $Root 'client-core\client-core.jar')) }
    $compile += ('@' + $sourceList)
    & javac @compile
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed: $module" }
    $resources = Join-Path $moduleDir 'src\main\resources'
    if (Test-Path $resources) { Copy-Item "$resources\*" $classes -Recurse -Force }
    $manifest = "Manifest-Version: 1.0`n"
    $packages = @{'client-gateway'='gateway';'narration-server'='narration';'audio-server'='audio';'client-server'='web'}
    if ($packages.ContainsKey($module)) {
        $manifest += "Main-Class: com.sgu.$($packages[$module]).Application`n"
        $manifest += "Class-Path: ../client-core/client-core.jar ../lib/mysql-connector-j-9.7.0.jar`n"
    }
    $manifest += "`n"
    $mf = Join-Path $moduleDir 'target\MANIFEST.MF'
    [IO.File]::WriteAllText($mf, $manifest, (New-Object Text.UTF8Encoding($false)))
    & jar --create --file (Join-Path $moduleDir "$module.jar") --manifest $mf -C $classes .
    if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed: $module" }
    Write-Host "Built $module" -ForegroundColor Green
}
Write-Host 'Build complete. Run start-demo.bat or start-live.bat.'
