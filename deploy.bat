@echo off
chcp 65001 >nul
echo ========================================
echo   XianCore 自动编译部署脚本
echo ========================================
echo.

:: 进入项目目录
cd /d D:\workspace\java\mc\frxx

echo [1/4] 正在编译 XianCore 插件...
cd XianCore
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 编译失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo ✅ 编译成功！
echo.

:: 检查目标 JAR 是否存在
if not exist "target\XianCore-1.0.0-SNAPSHOT.jar" (
    echo ❌ 找不到编译后的 JAR 文件！
    pause
    exit /b 1
)

echo [2/4] 正在备份旧版本...
if exist "D:\workspace\mc\乾坤生存R\plugins\XianCore.jar" (
    copy "D:\workspace\mc\乾坤生存R\plugins\XianCore.jar" "D:\workspace\mc\乾坤生存R\plugins\XianCore.jar.backup" >nul
    echo ✅ 已备份旧版本为 XianCore.jar.backup
) else (
    echo ℹ️  未找到旧版本（首次部署）
)

echo.
echo [3/4] 正在复制新版本到服务器...
copy /Y "target\XianCore-1.0.0-SNAPSHOT.jar" "D:\workspace\mc\乾坤生存R\plugins\XianCore.jar"

if %ERRORLEVEL% NEQ 0 (
    echo ❌ 复制失败！请检查服务器路径是否正确
    pause
    exit /b 1
)

echo ✅ 复制成功！
echo.

echo [4/4] 部署完成！
echo ========================================
echo.
echo 📦 新插件已部署到：
echo    D:\workspace\mc\乾坤生存R\plugins\XianCore.jar
echo.
echo ⚠️  重要提示：
echo    1. 请完全停止服务器（使用 stop 命令）
echo    2. 重新启动服务器
echo    3. 测试命令：/xiancore migrate --info
echo.
echo ✨ 新功能：YML到MySQL数据迁移工具
echo    - /xiancore migrate --info      查看迁移信息
echo    - /xiancore migrate --dry-run   预览迁移
echo    - /xiancore migrate confirm     执行迁移
echo.
echo ========================================
pause
