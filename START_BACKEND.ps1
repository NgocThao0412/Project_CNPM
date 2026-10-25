$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
Set-Location -LiteralPath $projectRoot

# Cấu hình dùng chung cho 5 backend.
$dbUser = Read-Host "Tai khoan MySQL (Enter de dung root)"
if ([string]::IsNullOrWhiteSpace($dbUser)) {
    $dbUser = "root"
}

$env:SGU_DB_USER = $dbUser
$env:SPRING_DATASOURCE_USERNAME = $dbUser
$env:SGU_DB_URL = "jdbc:mysql://127.0.0.1:3306/sgu_audio_guide"
$env:SPRING_DATASOURCE_URL = $env:SGU_DB_URL

$passwordInput = Read-Host "Mat khau MySQL" -AsSecureString
$env:SGU_DB_PASSWORD = [System.Net.NetworkCredential]::new(
    "", $passwordInput
).Password
Remove-Variable passwordInput

# Nhận key trong cửa sổ này nếu chưa có.
if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
    Read-Host "Sang AI Studio Copy API key, quay lai day nhan Enter"

    $keyText = (Get-Clipboard -Raw).Trim()

    if ([string]::IsNullOrWhiteSpace($keyText) -or
        $keyText -match '\s|[^\x21-\x7E]') {
        throw "Clipboard chua phai API key hop le."
    }

    $env:GEMINI_API_KEY = $keyText
    Remove-Variable keyText
}

$env:AUDIO_STORAGE_DIR = Join-Path $projectRoot "data\uploads"

$services = @(
    @{
        name = "auth-service"
        port = 8081
        jar = "auth-service-0.0.1-SNAPSHOT.jar"
    }
    @{
        name = "poi-service"
        port = 8082
        jar = "poi-service-0.0.1-SNAPSHOT.jar"
    }
    @{
        name = "narration-service"
        port = 8083
        jar = "narration-server-1.0.0.jar"
    }
    @{
        name = "audio-service"
        port = 8084
        jar = "audio-server-1.0.0.jar"
    }
    @{
        name = "visitor-service"
        port = 8085
        jar = "visitor-service-1.0.0.jar"
    }
)

foreach ($service in $services) {
    $listener = Get-NetTCPConnection `
        -State Listen `
        -LocalPort $service.port `
        -ErrorAction SilentlyContinue

    if ($listener) {
        Write-Host "Cong $($service.port) da duoc su dung, bo qua."
        continue
    }

    $module = Join-Path $projectRoot (
        "src\backend\" + $service.name
    )

    # Đóng gói code mới trước khi chạy.
    & "$projectRoot\mvnw.cmd" `
        -f "$module\pom.xml" `
        package `
        "-Dmaven.test.skip=true"

    if ($LASTEXITCODE -ne 0) {
        throw "Build that bai: $($service.name)"
    }

    $jarPath = Join-Path "$module\target" $service.jar

    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Khong tim thay JAR: $jarPath"
    }

    $safeRoot = $projectRoot.Replace("'", "''")
    $safeJar = $jarPath.Replace("'", "''")
    $title = "$($service.name) - $($service.port)"

    $command = "Set-Location -LiteralPath '$safeRoot'; " +
        "`$Host.UI.RawUI.WindowTitle = '$title'; " +
        "& java -jar '$safeJar'"

    Start-Process powershell.exe -ArgumentList @(
        "-NoProfile"
        "-NoExit"
        "-Command"
        "`"$command`""
    )
}

Write-Host "`nDa gui lenh khoi dong. Kiem tra log trong tung cua so."

Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in 8081,8082,8083,8084,8085 } |
    Select-Object LocalPort, OwningProcess |
    Sort-Object LocalPort