# XianCore Dashboard - PowerShell 部署脚本
# =============================================
#
# 使用方法:
#   1. 右键点击此文件，选择"使用 PowerShell 运行"
#   2. 或在 PowerShell 中执行: .\deploy.ps1
#
# 如果遇到执行策略错误，请以管理员身份运行 PowerShell 并执行:
#   Set-ExecutionPolicy RemoteSigned -Scope CurrentUser

param(
    [string]$Action = "menu"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# 颜色输出函数
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Type = "Info"
    )

    switch ($Type) {
        "Success" { Write-Host "[成功] $Message" -ForegroundColor Green }
        "Error"   { Write-Host "[错误] $Message" -ForegroundColor Red }
        "Warning" { Write-Host "[警告] $Message" -ForegroundColor Yellow }
        "Info"    { Write-Host "[信息] $Message" -ForegroundColor Cyan }
        default   { Write-Host $Message }
    }
}

# 检查 Docker 是否安装
function Test-Docker {
    try {
        $null = docker --version 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Docker command failed"
        }
    }
    catch {
        Write-ColorOutput "未检测到 Docker，请先安装 Docker Desktop" "Error"
        Write-Host "下载地址: https://www.docker.com/products/docker-desktop"
        Read-Host "按回车键继续"
        return $false
    }

    try {
        $null = docker-compose --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            return $true
        }

        $null = docker compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            return $true
        }

        throw "Docker Compose not found"
    }
    catch {
        Write-ColorOutput "未检测到 Docker Compose" "Error"
        Read-Host "按回车键继续"
        return $false
    }

    return $true
}

# 检查环境变量文件
function Test-EnvFile {
    if (-not (Test-Path ".env")) {
        Write-ColorOutput "未找到 .env 文件" "Warning"

        if (Test-Path ".env.docker.example") {
            Write-ColorOutput "正在从 .env.docker.example 创建 .env 文件..." "Info"
            Copy-Item ".env.docker.example" ".env"
            Write-ColorOutput ".env 文件已创建，请根据需要修改配置" "Success"
            Write-Host ""

            $edit = Read-Host "是否现在编辑 .env 文件? (y/n)"
            if ($edit -eq "y" -or $edit -eq "Y") {
                notepad .env
            }
        }
        else {
            Write-ColorOutput "未找到 .env.docker.example 模板文件" "Error"
            Read-Host "按回车键继续"
            return $false
        }
    }
    return $true
}

# 获取 Docker Compose 命令
function Get-DockerComposeCmd {
    try {
        $null = docker-compose --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            return "docker-compose"
        }
    }
    catch { }

    return "docker compose"
}

# 显示菜单
function Show-Menu {
    Clear-Host
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  XianCore Dashboard - 部署管理" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host " 1. 首次部署（配置环境并启动）"
    Write-Host " 2. 启动所有服务"
    Write-Host " 3. 停止所有服务"
    Write-Host " 4. 重启所有服务"
    Write-Host " 5. 查看服务状态"
    Write-Host " 6. 查看实时日志"
    Write-Host " 7. 重新构建并启动"
    Write-Host " 8. 停止并清理所有数据"
    Write-Host " 9. 备份数据库"
    Write-Host " 10. 恢复数据库"
    Write-Host " 11. 进入服务容器"
    Write-Host " 12. 查看服务健康状态"
    Write-Host " 0. 退出"
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

# 首次部署
function Start-FirstDeploy {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  首次部署 - 环境检查与配置"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }
    if (-not (Test-EnvFile)) { return }

    $composeCmd = Get-DockerComposeCmd

    Write-Host ""
    Write-ColorOutput "开始构建镜像..." "Info"

    try {
        Invoke-Expression "$composeCmd build"
        Write-ColorOutput "镜像构建完成" "Success"
    }
    catch {
        Write-ColorOutput "镜像构建失败: $_" "Error"
        Read-Host "按回车键继续"
        return
    }

    Write-Host ""
    Write-ColorOutput "启动服务..." "Info"

    try {
        Invoke-Expression "$composeCmd up -d"
        Write-ColorOutput "服务启动成功！" "Success"
        Write-Host ""
        Write-Host "访问地址:"
        Write-Host "  - 前端: http://localhost"
        Write-Host "  - 后端 API: http://localhost:8400"
        Write-Host ""
        Write-Host "查看日志: $composeCmd logs -f"
    }
    catch {
        Write-ColorOutput "服务启动失败: $_" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 启动服务
function Start-Services {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  启动所有服务"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd
    Write-ColorOutput "正在启动服务..." "Info"

    try {
        Invoke-Expression "$composeCmd up -d"
        Write-Host ""
        Write-ColorOutput "服务启动成功！" "Success"
        Write-Host ""
        Invoke-Expression "$composeCmd ps"
    }
    catch {
        Write-ColorOutput "服务启动失败: $_" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 停止服务
function Stop-Services {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  停止所有服务"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd
    Write-ColorOutput "正在停止服务..." "Info"

    Invoke-Expression "$composeCmd stop"

    Write-Host ""
    Write-ColorOutput "服务已停止" "Success"
    Write-Host ""
    Read-Host "按回车键继续"
}

# 重启服务
function Restart-Services {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  重启所有服务"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd
    Write-ColorOutput "正在重启服务..." "Info"

    Invoke-Expression "$composeCmd restart"

    Write-Host ""
    Write-ColorOutput "服务已重启" "Success"
    Write-Host ""
    Invoke-Expression "$composeCmd ps"
    Write-Host ""
    Read-Host "按回车键继续"
}

# 查看状态
function Show-Status {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  服务状态"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd
    Invoke-Expression "$composeCmd ps"

    Write-Host ""
    Read-Host "按回车键继续"
}

# 查看日志
function Show-Logs {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  实时日志（按 Ctrl+C 退出）"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd

    Write-Host "选择要查看的服务:"
    Write-Host "  1. 所有服务"
    Write-Host "  2. Backend"
    Write-Host "  3. Frontend"
    Write-Host "  4. MySQL"
    Write-Host ""

    $choice = Read-Host "请选择 (1-4)"

    switch ($choice) {
        "1" { Invoke-Expression "$composeCmd logs -f" }
        "2" { Invoke-Expression "$composeCmd logs -f backend" }
        "3" { Invoke-Expression "$composeCmd logs -f frontend" }
        "4" { Invoke-Expression "$composeCmd logs -f mysql" }
        default { Write-ColorOutput "无效选择" "Error" }
    }
}

# 重新构建
function Rebuild-Services {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  重新构建并启动"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd

    Write-ColorOutput "这将重新构建所有镜像，可能需要较长时间" "Warning"
    Write-Host ""
    $confirm = Read-Host "确认继续? (y/n)"

    if ($confirm -ne "y" -and $confirm -ne "Y") {
        return
    }

    try {
        Write-Host ""
        Write-ColorOutput "停止现有服务..." "Info"
        Invoke-Expression "$composeCmd down"

        Write-Host ""
        Write-ColorOutput "重新构建镜像..." "Info"
        Invoke-Expression "$composeCmd build --no-cache"

        Write-Host ""
        Write-ColorOutput "启动服务..." "Info"
        Invoke-Expression "$composeCmd up -d"

        Write-Host ""
        Write-ColorOutput "重建完成！" "Success"
        Write-Host ""
        Invoke-Expression "$composeCmd ps"
    }
    catch {
        Write-ColorOutput "操作失败: $_" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 清理数据
function Remove-AllData {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  停止并清理所有数据"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd

    Write-ColorOutput "这将删除所有容器、网络和数据卷（包括数据库数据）" "Warning"
    Write-ColorOutput "此操作不可恢复！" "Warning"
    Write-Host ""
    $confirm = Read-Host "确认继续? (y/n)"

    if ($confirm -ne "y" -and $confirm -ne "Y") {
        return
    }

    Write-Host ""
    Write-ColorOutput "停止并删除所有资源..." "Info"
    Invoke-Expression "$composeCmd down -v"

    Write-Host ""
    Write-ColorOutput "清理完成" "Success"
    Write-Host ""
    Read-Host "按回车键继续"
}

# 备份数据库
function Backup-Database {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  备份数据库"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd

    # 读取环境变量
    if (Test-Path ".env") {
        Get-Content ".env" | ForEach-Object {
            if ($_ -match '^([^=]+)=(.*)$') {
                Set-Variable -Name $matches[1] -Value $matches[2] -Scope Script
            }
        }
    }

    $dbName = if ($MYSQL_DATABASE) { $MYSQL_DATABASE } else { "xiancore" }
    $dbUser = if ($MYSQL_USER) { $MYSQL_USER } else { "securityuser" }
    $dbPass = if ($MYSQL_PASSWORD) { $MYSQL_PASSWORD } else { "security123" }

    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = "backup_$timestamp.sql"

    Write-ColorOutput "正在备份数据库到: $backupFile" "Info"

    try {
        $output = Invoke-Expression "$composeCmd exec -T mysql mysqldump -u $dbUser -p$dbPass $dbName"
        $output | Out-File -FilePath $backupFile -Encoding UTF8

        Write-Host ""
        Write-ColorOutput "数据库备份完成: $backupFile" "Success"
    }
    catch {
        Write-Host ""
        Write-ColorOutput "备份失败: $_" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 恢复数据库
function Restore-Database {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  恢复数据库"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    # 列出可用的备份文件
    $backups = Get-ChildItem -Path . -Filter "backup_*.sql" | Sort-Object LastWriteTime -Descending

    if ($backups.Count -eq 0) {
        Write-ColorOutput "未找到备份文件" "Error"
        Read-Host "按回车键继续"
        return
    }

    Write-Host "可用的备份文件:"
    Write-Host ""
    for ($i = 0; $i -lt $backups.Count; $i++) {
        Write-Host "  $($i + 1). $($backups[$i].Name) ($($backups[$i].LastWriteTime))"
    }
    Write-Host ""

    $choice = Read-Host "请选择要恢复的备份 (1-$($backups.Count))"
    $index = [int]$choice - 1

    if ($index -lt 0 -or $index -ge $backups.Count) {
        Write-ColorOutput "无效选择" "Error"
        Read-Host "按回车键继续"
        return
    }

    $backupFile = $backups[$index].Name

    Write-ColorOutput "警告: 这将覆盖当前数据库！" "Warning"
    $confirm = Read-Host "确认恢复 $backupFile ? (y/n)"

    if ($confirm -ne "y" -and $confirm -ne "Y") {
        return
    }

    $composeCmd = Get-DockerComposeCmd

    # 读取环境变量
    if (Test-Path ".env") {
        Get-Content ".env" | ForEach-Object {
            if ($_ -match '^([^=]+)=(.*)$') {
                Set-Variable -Name $matches[1] -Value $matches[2] -Scope Script
            }
        }
    }

    $dbName = if ($MYSQL_DATABASE) { $MYSQL_DATABASE } else { "xiancore" }
    $dbUser = if ($MYSQL_USER) { $MYSQL_USER } else { "securityuser" }
    $dbPass = if ($MYSQL_PASSWORD) { $MYSQL_PASSWORD } else { "security123" }

    Write-ColorOutput "正在恢复数据库..." "Info"

    try {
        Get-Content $backupFile | Invoke-Expression "$composeCmd exec -T mysql mysql -u $dbUser -p$dbPass $dbName"
        Write-Host ""
        Write-ColorOutput "数据库恢复完成" "Success"
    }
    catch {
        Write-Host ""
        Write-ColorOutput "恢复失败: $_" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 进入容器
function Enter-Container {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  进入服务容器"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    $composeCmd = Get-DockerComposeCmd

    Write-Host "选择要进入的容器:"
    Write-Host "  1. Backend"
    Write-Host "  2. Frontend"
    Write-Host "  3. MySQL"
    Write-Host ""

    $choice = Read-Host "请选择 (1-3)"

    switch ($choice) {
        "1" {
            Write-ColorOutput "进入 Backend 容器 (输入 exit 退出)..." "Info"
            Invoke-Expression "$composeCmd exec backend sh"
        }
        "2" {
            Write-ColorOutput "进入 Frontend 容器 (输入 exit 退出)..." "Info"
            Invoke-Expression "$composeCmd exec frontend sh"
        }
        "3" {
            Write-ColorOutput "进入 MySQL 容器 (输入 exit 退出)..." "Info"
            Invoke-Expression "$composeCmd exec mysql bash"
        }
        default {
            Write-ColorOutput "无效选择" "Error"
            Read-Host "按回车键继续"
        }
    }
}

# 查看健康状态
function Show-HealthStatus {
    Clear-Host
    Write-Host "========================================"
    Write-Host "  服务健康状态"
    Write-Host "========================================"
    Write-Host ""

    if (-not (Test-Docker)) { return }

    Write-ColorOutput "检查 Frontend..." "Info"
    try {
        $response = Invoke-WebRequest -Uri "http://localhost/health" -TimeoutSec 5 -UseBasicParsing
        Write-ColorOutput "Frontend: 健康 (HTTP $($response.StatusCode))" "Success"
    }
    catch {
        Write-ColorOutput "Frontend: 不可用" "Error"
    }

    Write-Host ""
    Write-ColorOutput "检查 Backend..." "Info"
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8400/api/health" -TimeoutSec 5 -UseBasicParsing
        Write-ColorOutput "Backend: 健康 (HTTP $($response.StatusCode))" "Success"
    }
    catch {
        Write-ColorOutput "Backend: 不可用" "Error"
    }

    Write-Host ""
    Read-Host "按回车键继续"
}

# 主菜单循环
function Start-MainLoop {
    while ($true) {
        Show-Menu
        $choice = Read-Host "请选择操作 (0-12)"

        switch ($choice) {
            "1"  { Start-FirstDeploy }
            "2"  { Start-Services }
            "3"  { Stop-Services }
            "4"  { Restart-Services }
            "5"  { Show-Status }
            "6"  { Show-Logs }
            "7"  { Rebuild-Services }
            "8"  { Remove-AllData }
            "9"  { Backup-Database }
            "10" { Restore-Database }
            "11" { Enter-Container }
            "12" { Show-HealthStatus }
            "0"  {
                Write-Host ""
                Write-Host "感谢使用！"
                Start-Sleep -Seconds 1
                exit 0
            }
            default {
                Write-ColorOutput "无效选择，请重试..." "Error"
                Start-Sleep -Seconds 2
            }
        }
    }
}

# 处理命令行参数
switch ($Action.ToLower()) {
    "start"   { Start-Services; exit 0 }
    "stop"    { Stop-Services; exit 0 }
    "restart" { Restart-Services; exit 0 }
    "status"  { Show-Status; exit 0 }
    "logs"    { Show-Logs; exit 0 }
    "build"   { Rebuild-Services; exit 0 }
    "backup"  { Backup-Database; exit 0 }
    "menu"    { Start-MainLoop }
    default   { Start-MainLoop }
}
