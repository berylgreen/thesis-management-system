@echo off
REM 学生数据导入脚本 - 使用正确的字符集
REM 作用：将 sql/import_students.sql 导入到数据库，确保中文不乱码

echo ===============================================
echo 学生数据导入工具
echo ===============================================
echo.

REM 检查 Docker 容器是否运行
docker ps | findstr thesis-mysql-dev > nul
if %errorlevel% neq 0 (
    echo [错误] MySQL 容器未运行，请先启动 Docker 服务
    echo 运行命令: docker-compose up -d
    exit /b 1
)

echo [步骤 1/3] 检查 SQL 文件...
if not exist sql\import_students.sql (
    echo [错误] 找不到 sql\import_students.sql 文件
    exit /b 1
)
echo [完成] SQL 文件存在

echo.
echo [步骤 2/3] 导入学生数据（使用 UTF-8 字符集）...
docker exec -i thesis-mysql-dev mysql -uroot -proot --default-character-set=utf8mb4 thesis_system < sql\import_students.sql
if %errorlevel% neq 0 (
    echo [错误] 数据导入失败
    exit /b 1
)
echo [完成] 数据导入成功

echo.
echo [步骤 3/3] 验证数据...
docker exec -i thesis-mysql-dev mysql -uroot -proot thesis_system -e "SELECT COUNT(*) as student_count FROM t_user WHERE role='STUDENT';"
echo.

echo ===============================================
echo 导入完成！
echo ===============================================
echo.
echo 提示：如果后端服务正在运行，建议重启以加载最新数据
echo.
pause
