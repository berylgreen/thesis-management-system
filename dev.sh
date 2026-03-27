#!/bin/bash
# 开发环境管理脚本 (Linux)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PATH="$SCRIPT_DIR/backend"
FRONTEND_PATH="$SCRIPT_DIR/frontend"
LOG_DIR="$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

show_status() {
    printf "\n"
    printf "${CYAN}--- Service Status ---${NC}\n"
    printf "${CYAN}Database:        H2 (embedded)${NC}\n"

    # 检查 Backend 端口 8080
    if ss -tlnp 2>/dev/null | grep -q ':8080 ' || lsof -i :8080 -sTCP:LISTEN >/dev/null 2>&1; then
        printf "Backend (8080):  ${GREEN}RUNNING${NC} -> http://localhost:8080/\n"
    else
        printf "Backend (8080):  ${RED}STOPPED${NC}\n"
    fi

    # 检查 Frontend 端口 5173
    if ss -tlnp 2>/dev/null | grep -q ':5173 ' || lsof -i :5173 -sTCP:LISTEN >/dev/null 2>&1; then
        printf "Frontend (5173): ${GREEN}RUNNING${NC} -> http://localhost:5173/\n"
    else
        printf "Frontend (5173): ${RED}STOPPED${NC}\n"
    fi
    printf "${CYAN}----------------------${NC}\n"
    printf "\n"
}

# 杀掉所有监听 517x 端口的 node 进程（Vite 开发服务器）
kill_all_vite() {
    local pids
    pids=$(ss -tlnp 2>/dev/null | grep -oP ':517[0-9]\b.*pid=\K[0-9]+' | sort -u)
    if [ -n "$pids" ]; then
        printf "${YELLOW}Killing Vite processes: $pids${NC}\n"
        echo "$pids" | xargs kill -9 2>/dev/null
        sleep 1
    fi
    rm -f "$LOG_DIR/frontend.pid"
}

# 杀掉监听 8080 端口的 java 进程（Spring Boot）
kill_all_backend() {
    local pids
    pids=$(ss -tlnp 2>/dev/null | grep -oP ':8080\b.*pid=\K[0-9]+' | sort -u)
    if [ -n "$pids" ]; then
        printf "${YELLOW}Killing Backend processes: $pids${NC}\n"
        echo "$pids" | xargs kill -9 2>/dev/null
        sleep 1
    fi
    rm -f "$LOG_DIR/backend.pid"
}

start_services() {
    printf "${GREEN}Starting Thesis Dev Environment (H2 embedded, no external dependencies)...${NC}\n"

    # 先停掉已有服务，防止僵尸进程
    printf "${YELLOW}Cleaning up any existing services...${NC}\n"
    kill_all_vite
    kill_all_backend

    # 检查并安装前端依赖
    if [ ! -d "$FRONTEND_PATH/node_modules" ]; then
        printf "${YELLOW}Frontend dependencies not found, running npm install...${NC}\n"
        cd "$FRONTEND_PATH" && npm install
        cd "$SCRIPT_DIR"
    fi

    # 启动 Backend
    printf "${CYAN}Starting Backend Service (check backend.log for output)...${NC}\n"
    cd "$BACKEND_PATH" && nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > "$LOG_DIR/backend.log" 2> "$LOG_DIR/backend.err.log" &
    echo $! > "$LOG_DIR/backend.pid"
    cd "$SCRIPT_DIR"

    # 启动 Frontend
    printf "${CYAN}Starting Frontend Service (check frontend.log for output)...${NC}\n"
    cd "$FRONTEND_PATH" && nohup npm run dev > "$LOG_DIR/frontend.log" 2> "$LOG_DIR/frontend.err.log" &
    echo $! > "$LOG_DIR/frontend.pid"
    cd "$SCRIPT_DIR"

    printf "${GREEN}Services started in background.${NC}\n"
    printf "PIDs saved to backend.pid and frontend.pid\n"
    printf "Logs:\n"
    printf "  Backend:  $LOG_DIR/backend.log\n"
    printf "  Frontend: $LOG_DIR/frontend.log\n"
}

stop_services() {
    printf "${YELLOW}Stopping Services...${NC}\n"

    # 先尝试通过 PID 文件中的进程组来停止
    kill_by_pid_file() {
        local pid_file="$1"
        local name="$2"
        if [ -f "$pid_file" ]; then
            local pid_target
            pid_target=$(cat "$pid_file")
            printf "${YELLOW}Stopping $name (PID: $pid_target)...${NC}\n"
            if kill -0 "$pid_target" 2>/dev/null; then
                # 尝试杀掉整个进程组
                kill -- -"$pid_target" 2>/dev/null || kill "$pid_target" 2>/dev/null
            fi
            rm -f "$pid_file"
        fi
    }

    kill_by_pid_file "$LOG_DIR/backend.pid" "Backend"
    kill_by_pid_file "$LOG_DIR/frontend.pid" "Frontend"

    # 兜底: 基于端口扫描，确保杀掉所有相关进程（包括端口漂移的僵尸进程）
    sleep 1
    kill_all_backend
    kill_all_vite
}

# 主逻辑
case "$1" in
    start)
        start_services
        printf "${YELLOW}Waiting for Backend to start (may take ~10s)...${NC}\n"
        sleep 10
        show_status
        ;;
    stop)
        stop_services
        sleep 1
        show_status
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        printf "${YELLOW}Waiting for Backend to start (may take ~10s)...${NC}\n"
        sleep 10
        show_status
        ;;
    status)
        show_status
        ;;
    *)
        printf "Usage: $0 {start|stop|restart|status}\n"
        exit 1
        ;;
esac
