# 开发环境管理脚本

param (
    [Parameter(Mandatory = $true)]
    [ValidateSet("start", "stop", "restart", "status")]
    [string]$Action
)

$backendPath = Resolve-Path ".\backend"
$frontendPath = Resolve-Path ".\frontend"
$logDir = Resolve-Path "."

function Show-Status {
    Write-Host "`n--- Service Status ---" -ForegroundColor Cyan
    Write-Host "Database:        H2 (embedded)" -ForegroundColor DarkCyan

    # Check Backend Port 8080
    if (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue) { Write-Host "Backend (8080):  RUNNING" -ForegroundColor Green } else { Write-Host "Backend (8080):  STOPPED" -ForegroundColor Red }

    # Check Frontend Port 5173
    if (Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue) { Write-Host "Frontend (5173): RUNNING" -ForegroundColor Green } else { Write-Host "Frontend (5173): STOPPED" -ForegroundColor Red }
    Write-Host "----------------------`n"
}

function Start-Services {
    Write-Host "Starting Thesis Dev Environment (H2 embedded, no external dependencies)..." -ForegroundColor Green

    # Start Backend with dev profile (H2)
    Write-Host "Starting Backend Service (check backend.log for output)..." -ForegroundColor Cyan
    $backendLog = "$logDir\backend.log"
    $backendErr = "$logDir\backend.err.log"

    $pBack = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d ""$backendPath"" && C:\tools\apache-maven-3.9.9\bin\mvn spring-boot:run -Dspring-boot.run.profiles=dev > ""$backendLog"" 2> ""$backendErr""" -WindowStyle Hidden -PassThru
    $pBack.Id | Out-File "$logDir\backend.pid"

    # Start Frontend
    Write-Host "Starting Frontend Service (check frontend.log for output)..." -ForegroundColor Cyan
    $frontendLog = "$logDir\frontend.log"
    $frontendErr = "$logDir\frontend.err.log"

    $pFront = Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d ""$frontendPath"" && npm run dev > ""$frontendLog"" 2> ""$frontendErr""" -WindowStyle Hidden -PassThru
    $pFront.Id | Out-File "$logDir\frontend.pid"

    Write-Host "Services started in background." -ForegroundColor Green
    Write-Host "PIDs saved to backend.pid and frontend.pid"
    Write-Host "Logs:"
    Write-Host "  Backend:  $backendLog"
    Write-Host "  Frontend: $frontendLog"
}

function Stop-Services {
    Write-Host "Stopping Services..." -ForegroundColor Yellow

    # Helper to kill by PID file
    function Kill-By-PidFile ($pidFile, $name) {
        if (Test-Path $pidFile) {
            $pidTarget = Get-Content $pidFile
            Write-Host "Stopping $name (PID: $pidTarget)..." -ForegroundColor Yellow
            try {
                Start-Process -FilePath "taskkill" -ArgumentList "/F /T /PID $pidTarget" -WindowStyle Hidden -Wait
                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
            } catch {
                Write-Host "Failed to stop $name specific process. It might have already exited." -ForegroundColor DarkGray
            }
        }
    }

    Kill-By-PidFile "$logDir\backend.pid" "Backend"
    Kill-By-PidFile "$logDir\frontend.pid" "Frontend"

    # Fallback: Kill by Port
    function Kill-Port ($p, $n) {
        $conn = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue
        if ($conn) {
            $pidTarget = $conn.OwningProcess
            Write-Host "Fallback: Stopping $n by port $p (PID: $pidTarget)..." -ForegroundColor Yellow
            Stop-Process -Id $pidTarget -Force -ErrorAction SilentlyContinue 
        }
    }

    Kill-Port 8080 "Backend"
    Kill-Port 5173 "Frontend"
}

if ($Action -eq "start") {
    Start-Services
    Show-Status
}
elseif ($Action -eq "stop") {
    Stop-Services
    Show-Status
}
elseif ($Action -eq "restart") {
    Stop-Services
    Start-Sleep -Seconds 2
    Start-Services
    Show-Status
}
elseif ($Action -eq "status") {
    Show-Status
}
