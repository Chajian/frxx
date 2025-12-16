#!/bin/bash

# XianCore Dashboard - Linux/Mac 一键部署脚本
# =============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[信息]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

log_error() {
    echo -e "${RED}[错误]${NC} $1"
}

# 检查 Docker 是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "未检测到 Docker，请先安装 Docker"
        echo "安装指南: https://docs.docker.com/engine/install/"
        exit 1
    fi

    if ! docker ps &> /dev/null; then
        log_error "Docker 未运行或权限不足"
        echo "请确保 Docker 服务已启动，并且当前用户有权限运行 Docker"
        echo "提示: 可以将当前用户添加到 docker 组: sudo usermod -aG docker \$USER"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "未检测到 Docker Compose"
        echo "安装指南: https://docs.docker.com/compose/install/"
        exit 1
    fi
}

# 检查并创建环境变量文件
check_env() {
    if [ ! -f ".env" ]; then
        log_warning "未找到 .env 文件"
        if [ -f ".env.docker.example" ]; then
            log_info "正在从 .env.docker.example 创建 .env 文件..."
            cp ".env.docker.example" ".env"
            log_success ".env 文件已创建，请根据需要修改配置"
            echo ""
            read -p "是否现在编辑 .env 文件? (y/n): " edit_env
            if [[ "$edit_env" =~ ^[Yy]$ ]]; then
                ${EDITOR:-nano} .env
            fi
        else
            log_error "未找到 .env.docker.example 模板文件"
            exit 1
        fi
    fi
}

# 获取 docker-compose 命令
get_docker_compose_cmd() {
    if command -v docker-compose &> /dev/null; then
        echo "docker-compose"
    else
        echo "docker compose"
    fi
}

# 显示菜单
show_menu() {
    clear
    echo ""
    echo "========================================"
    echo "  XianCore Dashboard - 部署管理"
    echo "========================================"
    echo ""
    echo " 1. 首次部署（配置环境并启动）"
    echo " 2. 启动所有服务"
    echo " 3. 停止所有服务"
    echo " 4. 重启所有服务"
    echo " 5. 查看服务状态"
    echo " 6. 查看实时日志"
    echo " 7. 重新构建并启动"
    echo " 8. 停止并清理所有数据"
    echo " 9. 备份数据库"
    echo " 10. 进入服务容器"
    echo " 0. 退出"
    echo ""
    echo "========================================"
    echo ""
}

# 首次部署
first_deploy() {
    clear
    echo "========================================"
    echo "  首次部署 - 环境检查与配置"
    echo "========================================"
    echo ""

    check_docker
    check_env

    echo ""
    log_info "开始构建镜像..."
    COMPOSE_CMD=$(get_docker_compose_cmd)

    if ! $COMPOSE_CMD build; then
        echo ""
        log_error "镜像构建失败"
        read -p "按回车键继续..."
        return 1
    fi

    log_success "镜像构建完成"
    echo ""
    log_info "启动服务..."

    if ! $COMPOSE_CMD up -d; then
        echo ""
        log_error "服务启动失败"
        read -p "按回车键继续..."
        return 1
    fi

    echo ""
    log_success "服务启动成功！"
    echo ""
    echo "访问地址:"
    echo "  - 前端: http://localhost"
    echo "  - 后端 API: http://localhost:8400"
    echo ""
    echo "查看日志: $COMPOSE_CMD logs -f"
    echo ""
    read -p "按回车键继续..."
}

# 启动服务
start_services() {
    clear
    echo "========================================"
    echo "  启动所有服务"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    log_info "正在启动服务..."
    if ! $COMPOSE_CMD up -d; then
        echo ""
        log_error "服务启动失败"
        read -p "按回车键继续..."
        return 1
    fi

    echo ""
    log_success "服务启动成功！"
    echo ""
    $COMPOSE_CMD ps
    echo ""
    read -p "按回车键继续..."
}

# 停止服务
stop_services() {
    clear
    echo "========================================"
    echo "  停止所有服务"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    log_info "正在停止服务..."
    $COMPOSE_CMD stop

    echo ""
    log_success "服务已停止"
    echo ""
    read -p "按回车键继续..."
}

# 重启服务
restart_services() {
    clear
    echo "========================================"
    echo "  重启所有服务"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    log_info "正在重启服务..."
    $COMPOSE_CMD restart

    echo ""
    log_success "服务已重启"
    echo ""
    $COMPOSE_CMD ps
    echo ""
    read -p "按回车键继续..."
}

# 查看状态
show_status() {
    clear
    echo "========================================"
    echo "  服务状态"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    $COMPOSE_CMD ps

    echo ""
    read -p "按回车键继续..."
}

# 查看日志
show_logs() {
    clear
    echo "========================================"
    echo "  实时日志（按 Ctrl+C 退出）"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    echo "选择要查看的服务:"
    echo "  1. 所有服务"
    echo "  2. Backend"
    echo "  3. Frontend"
    echo "  4. MySQL"
    echo ""

    read -p "请选择 (1-4): " log_choice

    case $log_choice in
        1) $COMPOSE_CMD logs -f ;;
        2) $COMPOSE_CMD logs -f backend ;;
        3) $COMPOSE_CMD logs -f frontend ;;
        4) $COMPOSE_CMD logs -f mysql ;;
        *) log_error "无效选择" ;;
    esac
}

# 重新构建
rebuild_services() {
    clear
    echo "========================================"
    echo "  重新构建并启动"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    log_warning "这将重新构建所有镜像，可能需要较长时间"
    echo ""
    read -p "确认继续? (y/n): " confirm

    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        return 0
    fi

    echo ""
    log_info "停止现有服务..."
    $COMPOSE_CMD down

    echo ""
    log_info "重新构建镜像..."
    $COMPOSE_CMD build --no-cache

    echo ""
    log_info "启动服务..."
    $COMPOSE_CMD up -d

    echo ""
    log_success "重建完成！"
    echo ""
    $COMPOSE_CMD ps
    echo ""
    read -p "按回车键继续..."
}

# 清理数据
clean_all() {
    clear
    echo "========================================"
    echo "  停止并清理所有数据"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    log_warning "这将删除所有容器、网络和数据卷（包括数据库数据）"
    log_warning "此操作不可恢复！"
    echo ""
    read -p "确认继续? (y/n): " confirm

    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        return 0
    fi

    echo ""
    log_info "停止并删除所有资源..."
    $COMPOSE_CMD down -v

    echo ""
    log_success "清理完成"
    echo ""
    read -p "按回车键继续..."
}

# 备份数据库
backup_database() {
    clear
    echo "========================================"
    echo "  备份数据库"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    # 读取环境变量
    if [ -f ".env" ]; then
        export $(grep -v '^#' .env | xargs)
    fi

    DB_NAME=${MYSQL_DATABASE:-xiancore}
    DB_USER=${MYSQL_USER:-securityuser}
    DB_PASS=${MYSQL_PASSWORD:-security123}

    BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"

    log_info "正在备份数据库到: $BACKUP_FILE"
    if $COMPOSE_CMD exec -T mysql mysqldump -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" > "$BACKUP_FILE"; then
        echo ""
        log_success "数据库备份完成: $BACKUP_FILE"
    else
        echo ""
        log_error "备份失败"
    fi

    echo ""
    read -p "按回车键继续..."
}

# 进入容器
exec_container() {
    clear
    echo "========================================"
    echo "  进入服务容器"
    echo "========================================"
    echo ""

    check_docker
    COMPOSE_CMD=$(get_docker_compose_cmd)

    echo "选择要进入的容器:"
    echo "  1. Backend"
    echo "  2. Frontend"
    echo "  3. MySQL"
    echo ""

    read -p "请选择 (1-3): " exec_choice

    case $exec_choice in
        1)
            log_info "进入 Backend 容器 (输入 exit 退出)..."
            $COMPOSE_CMD exec backend sh
            ;;
        2)
            log_info "进入 Frontend 容器 (输入 exit 退出)..."
            $COMPOSE_CMD exec frontend sh
            ;;
        3)
            log_info "进入 MySQL 容器 (输入 exit 退出)..."
            $COMPOSE_CMD exec mysql bash
            ;;
        *)
            log_error "无效选择"
            read -p "按回车键继续..."
            ;;
    esac
}

# 主循环
main() {
    while true; do
        show_menu
        read -p "请选择操作 (0-10): " choice

        case $choice in
            1) first_deploy ;;
            2) start_services ;;
            3) stop_services ;;
            4) restart_services ;;
            5) show_status ;;
            6) show_logs ;;
            7) rebuild_services ;;
            8) clean_all ;;
            9) backup_database ;;
            10) exec_container ;;
            0)
                echo ""
                echo "感谢使用！"
                exit 0
                ;;
            *)
                log_error "无效选择，请重试..."
                sleep 2
                ;;
        esac
    done
}

# 运行主程序
main
