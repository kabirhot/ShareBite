@echo off
rem Compiles and starts the ShareBite backend (Windows). Run it from the backend folder.
cd /d "%~dp0"

rem ---- 1. MySQL JDBC driver: download it once if it is not in the lib folder ----
set JAR=lib\mysql-connector-j-8.4.0.jar
set JAR_URL=https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
set JAR_SHA1=b1bc0f47bcad26ad5f9bceefb63fcb920d868fca
if not exist lib mkdir lib
if not exist "%JAR%" (
    echo Downloading the MySQL JDBC driver...
    powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing '%JAR_URL%' -OutFile '%JAR%'"
    powershell -NoProfile -Command "if ((Get-FileHash '%JAR%' -Algorithm SHA1).Hash -ne '%JAR_SHA1%') { Remove-Item '%JAR%'; exit 1 }"
    if errorlevel 1 (
        echo Could not download the driver. See lib\README.txt to get it by hand.
        pause
        exit /b 1
    )
)

rem ---- 2. Database settings: ask for the MySQL password the first time and save it ----
if not exist db.properties (
    echo.
    echo First-time setup: ShareBite needs your MySQL password to connect to the database.
    echo It is saved only in backend\db.properties on this computer, never uploaded to GitHub.
    echo.
    powershell -NoProfile -Command "$u = Read-Host 'MySQL user (press Enter for root)'; if (-not $u) { $u = 'root' }; $s = Read-Host 'MySQL password' -AsSecureString; $p = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)); $p = $p.Replace('\', '\\'); Set-Content -Path 'db.properties' -Encoding ASCII -Value @('db.url=jdbc:mysql://localhost:3306/sharebite?createDatabaseIfNotExist=true', ('db.user=' + $u), ('db.password=' + $p))"
    if not exist db.properties (
        echo Could not save db.properties. Copy db.properties.example to db.properties and type your password in it.
        pause
        exit /b 1
    )
    echo Saved. If the password was wrong, delete db.properties and run this file again.
    echo.
)

rem ---- 3. Compile ----
if not exist out mkdir out
javac -cp "lib/*" -d out src\sharebite\*.java
if errorlevel 1 (
    echo.
    echo Compile failed. Make sure JDK 17 or newer is installed: java -version
    pause
    exit /b 1
)

rem ---- 4. Start: open the website a few seconds after the server starts ----
start "" /min cmd /c "timeout /t 4 /nobreak >nul & start http://localhost:8080"
rem "out;lib/*" = our compiled classes + the MySQL JDBC driver jar
rem Windows-ROOT = trust the same certificates as Windows (needed when antivirus scans email)
java -Djavax.net.ssl.trustStoreType=Windows-ROOT -cp "out;lib/*" sharebite.ShareBiteServer
echo.
echo The server has stopped. If it said it could not connect to MySQL:
echo  - check that MySQL is running (Windows Services: MySQL80), and
echo  - check the password: delete db.properties and run this file again to re-enter it.
pause
