$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$repository = "NgocThao0412/Project_CNPM"

# Chọn bản push lên main gần nhất đã vượt qua CI.
$runJson = & gh run list `
    --repo $repository `
    --workflow backend-ci.yml `
    --branch main `
    --event push `
    --status success `
    --limit 1 `
    --json databaseId,headSha,url

if ($LASTEXITCODE -ne 0) {
    throw "Khong lay duoc ket qua CI."
}

$runs = @($runJson | ConvertFrom-Json)

if ($runs.Count -eq 0) {
    throw "Chua co ban CI thanh cong tren main."
}

$run = $runs[0]
$commit = $run.headSha

if ($commit -notmatch '^[0-9a-f]{40}$') {
    throw "Ma commit khong hop le."
}

Write-Host "Ban CI duoc chon: $commit"
Write-Host $run.url

# Mỗi lần tải vào thư mục mới, không ghi đè JAR đang chạy.
$folderName = $commit.Substring(0, 12) + "-" +
    [Guid]::NewGuid().ToString("N").Substring(0, 8)

$releaseDir = Join-Path $projectRoot ".deploy\releases\$folderName"
New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null

& gh run download $run.databaseId `
    --repo $repository `
    --name "backend-release-$commit" `
    --dir $releaseDir

if ($LASTEXITCODE -ne 0) {
    throw "Khong tai duoc artifact. Kiem tra goi con han hay khong."
}

$savedCommit = (
    Get-Content (Join-Path $releaseDir "commit.txt") -Raw
).Trim()

if ($savedCommit -ne $commit) {
    throw "Commit trong goi khong khop voi CI."
}

$expectedJars = @(
    "auth-service-0.0.1-SNAPSHOT.jar"
    "poi-service-0.0.1-SNAPSHOT.jar"
    "narration-server-1.0.0.jar"
    "audio-server-1.0.0.jar"
    "visitor-service-1.0.0.jar"
)

$verified = @{}
$checksumPath = Join-Path $releaseDir "SHA256SUMS"

foreach ($line in Get-Content $checksumPath) {
    if ($line -notmatch '^([a-fA-F0-9]{64})\s+(.+)$') {
        throw "Dong checksum sai dinh dang."
    }

    $expectedHash = $Matches[1]
    $fileName = $Matches[2].Trim()

    if ($fileName -notin $expectedJars -or
        $verified.ContainsKey($fileName)) {
        throw "Ten file khong hop le hoac bi trung: $fileName"
    }

    $filePath = Join-Path $releaseDir $fileName
    $actualHash = (Get-FileHash $filePath -Algorithm SHA256).Hash

    if ($actualHash -ne $expectedHash) {
        throw "Checksum khong khop: $fileName"
    }

    $verified[$fileName] = $true
    Write-Host "OK: $fileName"
}

if ($verified.Count -ne $expectedJars.Count) {
    throw "Goi trien khai chua du 5 JAR."
}

Write-Host "`nTAI VA KIEM TRA THANH CONG" -ForegroundColor Green
Write-Host "Thu muc: $releaseDir"
# ===== CAU HINH LAPTOP =====

$dbUser = Read-Host "Tai khoan MySQL (Enter de dung root)"
if ([string]::IsNullOrWhiteSpace($dbUser)) {
    $dbUser = "root"
}

$passwordInput = Read-Host "Mat khau MySQL" -AsSecureString
$dbPassword = [Net.NetworkCredential]::new("", $passwordInput).Password

$env:SGU_DB_USER = $dbUser
$env:SPRING_DATASOURCE_USERNAME = $dbUser
$env:SGU_DB_PASSWORD = $dbPassword
$env:SPRING_DATASOURCE_PASSWORD = $dbPassword
$env:SGU_DB_URL = "jdbc:mysql://127.0.0.1:3306/sgu_audio_guide?connectionTimeZone=UTC"
$env:SPRING_DATASOURCE_URL = $env:SGU_DB_URL
$env:AUDIO_STORAGE_DIR = Join-Path $projectRoot "data\uploads"

Remove-Variable passwordInput, dbPassword

if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
    Read-Host "Copy Gemini API key, quay lai day nhan Enter"
    $keyText = (Get-Clipboard -Raw).Trim()

    if ([string]::IsNullOrWhiteSpace($keyText) -or
        $keyText -match '\s|[^\x21-\x7E]') {
        throw "Clipboard chua co Gemini key hop le."
    }

    $env:GEMINI_API_KEY = $keyText
    Remove-Variable keyText
}

$javaPath = "C:\Program Files\Java\jdk-24\bin\java.exe"

if (-not (Test-Path -LiteralPath $javaPath)) {
    throw "Khong tim thay Java: $javaPath"
}

$services = @(
    @{ port=8081; jar="auth-service-0.0.1-SNAPSHOT.jar" },
    @{ port=8082; jar="poi-service-0.0.1-SNAPSHOT.jar" },
    @{ port=8083; jar="narration-server-1.0.0.jar" },
    @{ port=8084; jar="audio-server-1.0.0.jar" },
    @{ port=8085; jar="visitor-service-1.0.0.jar" }
)

# ===== SAO LUU CAC JAR DANG CHAY =====

$backupDir = Join-Path $releaseDir "previous"
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

$oldProcesses = @()
$oldServices = @()

foreach ($service in $services) {
    $owners = @(
        Get-NetTCPConnection -State Listen `
            -LocalPort $service.port -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    )

    if ($owners.Count -ne 1) {
        throw "Can co dung mot backend dang chay o cong $($service.port)."
    }

    $process = Get-CimInstance Win32_Process `
        -Filter "ProcessId=$($owners[0])"

    if ($process.Name -ne "java.exe" -or
        $process.CommandLine -notmatch '-jar\s+(?:"([^"]+)"|(\S+))') {
        throw "Khong nhan dien duoc backend o cong $($service.port)."
    }

    $oldJar = $Matches[1]
    if ([string]::IsNullOrWhiteSpace($oldJar)) {
        $oldJar = $Matches[2]
    }

    # Chi chap nhan duong dan tuyet doi de tranh sao luu nham.
    if (-not [IO.Path]::IsPathRooted($oldJar)) {
        throw "JAR cong $($service.port) dang dung duong dan tuong doi. Gui minh loi nay."
    }

    if ([IO.Path]::GetFileName($oldJar) -ne $service.jar) {
        throw "Cong $($service.port) khong phai service mong doi."
    }

    $oldJar = [IO.Path]::GetFullPath($oldJar)
    $allowedProject = $oldJar.StartsWith(
        $projectRoot.TrimEnd('\') + '\',
        [StringComparison]::OrdinalIgnoreCase
    )
    $allowedInitial = $oldJar.StartsWith(
        "D:\SGU\backend-release\",
        [StringComparison]::OrdinalIgnoreCase
    )

    if (-not ($allowedProject -or $allowedInitial)) {
        throw "JAR nam ngoai thu muc trien khai cua du an: $oldJar"
    }

    $backupJar = Join-Path $backupDir $service.jar
    Copy-Item -LiteralPath $oldJar -Destination $backupJar

    if ((Get-FileHash $oldJar).Hash -ne
        (Get-FileHash $backupJar).Hash) {
        throw "Sao luu JAR khong khop: $oldJar"
    }

    $oldProcesses += [PSCustomObject]@{
        ProcessId = $process.ProcessId
        Created = $process.CreationDate
    }

    $oldServices += [PSCustomObject]@{
        port = $service.port
        path = $backupJar
    }
}

# ===== HAM KHOI DONG VA KIEM TRA =====

$script:startedBackends = @()

function Start-BackendSet($items, $logDir) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null

    foreach ($item in $items) {
        $listener = Get-NetTCPConnection -State Listen `
            -LocalPort $item.port -ErrorAction SilentlyContinue

        if ($listener) {
            throw "Cong $($item.port) van dang duoc su dung."
        }

        $process = Start-Process -FilePath $javaPath `
            -ArgumentList ('-jar "' + $item.path + '"') `
            -WorkingDirectory $projectRoot `
            -RedirectStandardOutput (Join-Path $logDir "$($item.port).log") `
            -RedirectStandardError (Join-Path $logDir "$($item.port).error.log") `
            -WindowStyle Hidden `
            -PassThru

                $script:startedBackends += [PSCustomObject]@{
            Port = $item.port
            ProcessId = [int]$process.Id
            Process = $process
        }

        Write-Host "Khoi dong cong $($item.port), PID $($process.Id)"
    }
}

function Get-HttpStatus($url) {
    try {
        $response = Invoke-WebRequest -Uri $url `
            -UseBasicParsing -TimeoutSec 3
        return [int]$response.StatusCode
    } catch {
        if ($null -ne $_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

function Wait-BackendSet {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $lastStatus = ""

    $checks = @(
        @{ url="http://localhost:8081/api/auth/me"; status=401 },
        @{ url="http://localhost:8082/api/pois"; status=200 },
        @{ url="http://localhost:8083/api/languages"; status=200 },
        @{ url="http://localhost:8084/api/audio?contentId=1"; status=200 },
        @{ url="http://localhost:8085/api/admin/visitor-stats"; status=401 }
    )

    while ($timer.Elapsed.TotalSeconds -lt 120) {
        $allListening = $true

        foreach ($item in $script:startedBackends) {
            $item.Process = Get-Process -Id $item.ProcessId -ErrorAction Stop

            if ($item.Process.HasExited) {
                throw "Backend cong $($item.Port) da dung. Xem log."
            }

            $owners = @(
                Get-NetTCPConnection -State Listen `
                    -LocalPort $item.Port `
                    -ErrorAction SilentlyContinue |
                    Select-Object -ExpandProperty OwningProcess -Unique
            )

            if ($item.Process.Id -notin $owners) {
                $allListening = $false
            }
        }

        if ($allListening) {
            $healthy = $true
            $statuses = @()

            foreach ($check in $checks) {
                $actual = Get-HttpStatus $check.url
                $statuses += "$actual $($check.url)"

                if ($actual -ne $check.status) {
                    $healthy = $false
                }
            }

            $lastStatus = $statuses -join "`n"

            if ($healthy) {
                Write-Host $lastStatus
                return
            }
        }

        Start-Sleep -Seconds 2
    }

    throw "Backend chua san sang.`n$lastStatus"
}

function Stop-StartedBackends {
    foreach ($item in $script:startedBackends) {
        $item.Process = Get-Process -Id $item.ProcessId -ErrorAction Stop

        if (-not $item.Process.HasExited) {
            $item.Process.Kill()

            if (-not $item.Process.WaitForExit(10000)) {
                throw "Khong dung duoc backend cong $($item.Port)."
            }
        }
    }

    # Chờ Windows giải phóng cổng sau khi tiến trình dừng.
    $timer = [Diagnostics.Stopwatch]::StartNew()

    do {
        $remaining = @(
            Get-NetTCPConnection -State Listen `
                -ErrorAction SilentlyContinue |
                Where-Object {
                    $_.LocalPort -in 8081,8082,8083,8084,8085
                }
        )

        if ($remaining.Count -eq 0) {
            return
        }

        Start-Sleep -Milliseconds 500
    } while ($timer.Elapsed.TotalSeconds -lt 15)

    $ports = ($remaining.LocalPort | Sort-Object -Unique) -join ", "
    throw "Cac cong van dang duoc su dung: $ports"
}

# ===== CAP NHAT; LOI THI KHOI PHUC BAN CU =====

$newServices = @(
    foreach ($service in $services) {
        [PSCustomObject]@{
            port = $service.port
            path = Join-Path $releaseDir $service.jar
        }
    }
)

$replacementStarted = $false

try {
    # Kiem tra lai PID truoc khi dung.
    foreach ($old in $oldProcesses) {
        $current = Get-CimInstance Win32_Process `
            -Filter "ProcessId=$($old.ProcessId)"

        if (-not $current -or $current.CreationDate -ne $old.Created) {
            throw "Tien trinh da thay doi. Hay chay lai script."
        }
    }

    $replacementStarted = $true

    foreach ($old in $oldProcesses) {
        $process = Get-Process -Id $old.ProcessId -ErrorAction SilentlyContinue

        if ($process) {
            $process.Kill()
            if (-not $process.WaitForExit(10000)) {
                throw "Khong dung duoc PID $($old.ProcessId)."
            }
        }
    }

    Start-BackendSet $newServices (Join-Path $releaseDir "logs")
    Wait-BackendSet

    [PSCustomObject]@{
        commit = $commit
        deployedAt = [DateTimeOffset]::Now.ToString("o")
        releaseDir = $releaseDir
        previousDir = $backupDir
    } | ConvertTo-Json |
        Set-Content (Join-Path $projectRoot ".deploy\current.json") -Encoding UTF8

    Write-Host "`nTRIEN KHAI THANH CONG: $commit" -ForegroundColor Green
    Write-Host "Log: $(Join-Path $releaseDir 'logs')"
} catch {
    $deployError = $_.Exception.Message
    Write-Host "Trien khai loi: $deployError" -ForegroundColor Red

    if ($replacementStarted) {
        try {
            Stop-StartedBackends

            # Chi khoi phuc khi cac cong deu da trong.
            foreach ($service in $services) {
                if (Get-NetTCPConnection -State Listen `
                    -LocalPort $service.port -ErrorAction SilentlyContinue) {
                    throw "Cong $($service.port) chua trong."
                }
            }

            $script:startedBackends = @()
            Start-BackendSet $oldServices (Join-Path $releaseDir "rollback-logs")
            Wait-BackendSet

            Write-Host "DA KHOI PHUC BAN CU." -ForegroundColor Yellow
        } catch {
            Write-Host "Khoi phuc chua thanh cong: $($_.Exception.Message)" `
                -ForegroundColor Red
            Write-Host "Ban sao JAR cu: $backupDir"
        }
    }

    throw $deployError
}