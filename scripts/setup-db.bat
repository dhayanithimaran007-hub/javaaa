@echo off
setlocal

echo =======================================================================
echo Smart Campus Database Setup Utility (MySQL)
echo =======================================================================

set /p DB_USER="Enter MySQL Username [root]: " || set DB_USER=root
if "%DB_USER%"=="" set DB_USER=root

set /p DB_PASS="Enter MySQL Password: "

echo.
echo Initializing schema (schema.sql)...
mysql -u %DB_USER% -p%DB_PASS% < "%~dp0\..\src\main\resources\db\schema.sql"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to execute schema.sql. Please verify your MySQL credentials.
    pause
    exit /b 1
)

echo Initializing seed data (seed.sql)...
mysql -u %DB_USER% -p%DB_PASS% < "%~dp0\..\src\main\resources\db\seed.sql"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to execute seed.sql.
    pause
    exit /b 1
)

echo.
echo [SUCCESS] MySQL database 'smart_campus_db' initialized successfully!
echo You may now run 'run.bat' to start the desktop application.
echo =======================================================================
pause
endlocal
