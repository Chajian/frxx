@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: XianCore Dashboard - Windows 一键部署脚本
:: =============================================

set SCRIPT_DIR=%~dp0
cd /d %SCRIPT_DIR%

:MENU
cls
echo.
echo ========================================
echo   XianCore Dashboard - 部署管理
echo ========================================
echo.
echo  1. 首次部署（配置环境并启动）
echo  2. 启动所有服务
echo  3. 停止所有服务
echo  4. 重启所有服务
echo  5. 查看服务状态
echo  6. 查看实时日志
echo  7. 重新构建并启动
echo  8. 停止并清理所有数据
echo  9. 备份数据库
echo  10. 进入服务容器
echo  0. 退出
echo.
echo ========================================
echo.

set /p choice=请选择操作 (0-10):

if "%choice%"=="1" goto FIRST_DEPLOY
if "%choice%"=="2" goto START
if "%choice%"=="3" goto STOP
if "%choice%"=="4" goto RESTART
if "%choice%"=="5" goto STATUS
if "%choice%"=="6" goto LOGS
if "%choice%"=="7" goto REBUILD
if "%choice%"=="8" goto CLEAN
if "%choice%"=="9" goto BACKUP
if "%choice%"=="10" goto EXEC
if "%choice%"=="0" goto EXIT

echo 无效选择，请重试...
timeout /t 2 >nul
goto MENU

:CHECK_DOCKER
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Docker，请先安装 Docker Desktop
    echo 下载地址: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo [错误] 未检测到 Docker Compose
        pause
        exit /b 1
    )
)
exit /b 0

:CHECK_ENV
if not exist ".env" (
    echo [警告] 未找到 .env 文件
    if exist ".env.docker.example" (
        echo [信息] 正在从 .env.docker.example 创建 .env 文件...
        copy ".env.docker.example" ".env" >nul
        echo [成功] .env 文件已创建，请根据需要修改配置
        echo.
        echo 按任意键继续编辑 .env 文件...
        pause >nul
        notepad .env
    ) else (
        echo [错误] 未找到 .env.docker.example 模板文件
        pause
        exit /b 1
    )
)
exit /b 0

:FIRST_DEPLOY
cls
echo ========================================
echo   首次部署 - 环境检查与配置
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

call :CHECK_ENV
if errorlevel 1 goto MENU

echo.
echo [信息] 开始构建镜像...
docker-compose build

if errorlevel 1 (
    echo.
    echo [错误] 镜像构建失败
    pause
    goto MENU
)

echo.
echo [成功] 镜像构建完成
echo.
echo [信息] 启动服务...
docker-compose up -d

if errorlevel 1 (
    echo.
    echo [错误] 服务启动失败
    pause
    goto MENU
)

echo.
echo [成功] 服务启动成功！
echo.
echo 访问地址:
echo   - 前端: http://localhost
echo   - 后端 API: http://localhost:8400
echo.
echo 查看日志: docker-compose logs -f
echo.
pause
goto MENU

:START
cls
echo ========================================
echo   启动所有服务
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo [信息] 正在启动服务...
docker-compose up -d

if errorlevel 1 (
    echo.
    echo [错误] 服务启动失败
    pause
    goto MENU
)

echo.
echo [成功] 服务启动成功！
echo.
docker-compose ps
echo.
pause
goto MENU

:STOP
cls
echo ========================================
echo   停止所有服务
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo [信息] 正在停止服务...
docker-compose stop

echo.
echo [成功] 服务已停止
echo.
pause
goto MENU

:RESTART
cls
echo ========================================
echo   重启所有服务
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo [信息] 正在重启服务...
docker-compose restart

echo.
echo [成功] 服务已重启
echo.
docker-compose ps
echo.
pause
goto MENU

:STATUS
cls
echo ========================================
echo   服务状态
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

docker-compose ps

echo.
echo 按任意键返回菜单...
pause >nul
goto MENU

:LOGS
cls
echo ========================================
echo   实时日志（按 Ctrl+C 退出）
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo 选择要查看的服务:
echo   1. 所有服务
echo   2. Backend
echo   3. Frontend
echo   4. MySQL
echo.

set /p log_choice=请选择 (1-4):

if "%log_choice%"=="1" docker-compose logs -f
if "%log_choice%"=="2" docker-compose logs -f backend
if "%log_choice%"=="3" docker-compose logs -f frontend
if "%log_choice%"=="4" docker-compose logs -f mysql

goto MENU

:REBUILD
cls
echo ========================================
echo   重新构建并启动
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo [警告] 这将重新构建所有镜像，可能需要较长时间
echo.
set /p confirm=确认继续? (y/n):

if /i not "%confirm%"=="y" goto MENU

echo.
echo [信息] 停止现有服务...
docker-compose down

echo.
echo [信息] 重新构建镜像...
docker-compose build --no-cache

echo.
echo [信息] 启动服务...
docker-compose up -d

echo.
echo [成功] 重建完成！
echo.
docker-compose ps
echo.
pause
goto MENU

:CLEAN
cls
echo ========================================
echo   停止并清理所有数据
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo [警告] 这将删除所有容器、网络和数据卷（包括数据库数据）
echo [警告] 此操作不可恢复！
echo.
set /p confirm=确认继续? (y/n):

if /i not "%confirm%"=="y" goto MENU

echo.
echo [信息] 停止并删除所有资源...
docker-compose down -v

echo.
echo [成功] 清理完成
echo.
pause
goto MENU

:BACKUP
cls
echo ========================================
echo   备份数据库
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

for /f "tokens=2 delims==" %%a in ('findstr "MYSQL_DATABASE" .env') do set DB_NAME=%%a
for /f "tokens=2 delims==" %%a in ('findstr "MYSQL_USER" .env') do set DB_USER=%%a
for /f "tokens=2 delims==" %%a in ('findstr "MYSQL_PASSWORD" .env') do set DB_PASS=%%a

set BACKUP_FILE=backup_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
set BACKUP_FILE=%BACKUP_FILE: =0%

echo [信息] 正在备份数据库到: %BACKUP_FILE%
docker-compose exec -T mysql mysqldump -u %DB_USER% -p%DB_PASS% %DB_NAME% > %BACKUP_FILE%

if errorlevel 1 (
    echo.
    echo [错误] 备份失败
    pause
    goto MENU
)

echo.
echo [成功] 数据库备份完成: %BACKUP_FILE%
echo.
pause
goto MENU

:EXEC
cls
echo ========================================
echo   进入服务容器
echo ========================================
echo.

call :CHECK_DOCKER
if errorlevel 1 goto MENU

echo 选择要进入的容器:
echo   1. Backend
echo   2. Frontend
echo   3. MySQL
echo.

set /p exec_choice=请选择 (1-3):

if "%exec_choice%"=="1" (
    echo.
    echo [信息] 进入 Backend 容器 (输入 exit 退出)...
    docker-compose exec backend sh
)
if "%exec_choice%"=="2" (
    echo.
    echo [信息] 进入 Frontend 容器 (输入 exit 退出)...
    docker-compose exec frontend sh
)
if "%exec_choice%"=="3" (
    echo.
    echo [信息] 进入 MySQL 容器 (输入 exit 退出)...
    docker-compose exec mysql bash
)

goto MENU

:EXIT
echo.
echo 感谢使用！
timeout /t 2 >nul
exit /b 0
