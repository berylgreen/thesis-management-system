# 开发环境管理脚本 (Unified)

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
    
    # Check Docker
    $dockerRunning = (docker ps -q -f "name=thesis-mysql-dev" | Measure-Object).Count -gt 0
    if ($dockerRunning) { Write-Host "Docker Services: RUNNING" -ForegroundColor Green } else { Write-Host "Docker Services: STOPPED" -ForegroundColor Red }

    # Check Backend Port 8080
    if (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue) { Write-Host "Backend (8080):  RUNNING" -ForegroundColor Green } else { Write-Host "Backend (8080):  STOPPED" -ForegroundColor Red }

    # Check Frontend Port 3000
    if (Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue) { Write-Host "Frontend (3000): RUNNING" -ForegroundColor Green } else { Write-Host "Frontend (3000): STOPPED" -ForegroundColor Red }
    Write-Host "----------------------`n"
}

function Start-Services {
    Write-Host "Starting Thesis Development Environment..." -ForegroundColor Green

    # 1. Start Docker
    Write-Host "Starting Docker containers..." -ForegroundColor Cyan
    docker-compose -f docker-compose.dev.yml up -d

    # 2. Start Backend
    Write-Host "Starting Backend Service (check backend.log for output)..." -ForegroundColor Cyan
    $backendLog = "$logDir\backend.log"
    $backendErr = "$logDir\backend.err.log"
    # Note: RedirectStandardOutput/Error requires the process to be started differently or wrapped. 
    # Using Start-Process with WindowStyle Hidden is tricky with redirection.
    # We will use cmd /c to handle redirection easily.
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d ""$backendPath"" && mvn spring-boot:run > ""$backendLog"" 2> ""$backendErr""" -WindowStyle Hidden
    
    # 3. Start Frontend
    Write-Host "Starting Frontend Service (check frontend.log for output)..." -ForegroundColor Cyan
    $frontendLog = "$logDir\frontend.log"
    $frontendErr = "$logDir\frontend.err.log"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c cd /d ""$frontendPath"" && npm run dev > ""$frontendLog"" 2> ""$frontendErr""" -WindowStyle Hidden

    Write-Host "Services started in background." -ForegroundColor Green
    Write-Host "Logs:"
    Write-Host "  Backend:  $backendLog"
    Write-Host "  Frontend: $frontendLog"
}

function Stop-Services {
    Write-Host "Stopping Services..." -ForegroundColor Yellow
    
    # Stop Docker
    docker-compose -f docker-compose.dev.yml down

    # Kill by Port function
    function Kill-Port ($p, $n) {
        $conn = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue
        if ($conn) {
            $pidTarget = $conn.OwningProcess
            Write-Host "Stopping $n (PID: $pidTarget)..." -ForegroundColor Yellow
            Stop-Process -Id $pidTarget -Force -ErrorAction SilentlyContinue 
        }
        else {
            Write-Host "$n is not running." -ForegroundColor DarkGray
        }
    }

    Kill-Port 8080 "Backend"
    Kill-Port 3000 "Frontend"
    
    # Cleanup node/java processes started by us might be tricky if ports aren't immediately bound or if spawned as subprocesses. 
    # But port killing is usually reliable for web servers.
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
    Start-Interval -Seconds 2
    Start-Services
    Show-Status
}
elseif ($Action -eq "status") {
    Show-Status
}
