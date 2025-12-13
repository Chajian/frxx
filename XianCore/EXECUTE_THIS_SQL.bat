@echo off
echo ============================================
echo 创建 Boss配置表
echo ============================================
echo.

REM 请根据实际情况修改以下MySQL连接信息
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set MYSQL_DATABASE=xiancore

echo 正在连接MySQL数据库...
echo 数据库: %MYSQL_DATABASE%
echo 用户: %MYSQL_USER%
echo.

mysql -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DATABASE% < create_boss_config_tables.sql

if %errorlevel% == 0 (
    echo.
    echo ============================================
    echo ✓ Boss配置表创建成功！
    echo ============================================
    echo.
    echo 现在可以重新执行迁移：
    echo   /xiancore migrate confirm
    echo.
) else (
    echo.
    echo ============================================
    echo ✗ 创建失败，请检查MySQL连接信息
    echo ============================================
    echo.
    echo 请手动修改此文件中的：
    echo   MYSQL_USER (当前: %MYSQL_USER%)
    echo   MYSQL_PASSWORD (当前: %MYSQL_PASSWORD%)
    echo   MYSQL_DATABASE (当前: %MYSQL_DATABASE%)
    echo.
)

pause
