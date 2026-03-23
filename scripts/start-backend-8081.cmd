@echo off
setlocal
cd /d C:\NUCOSMOS_POS\backend
set DB_HOST=localhost
set DB_PORT=5433
set DB_NAME=nucosmos_pos
set DB_USERNAME=nucosmos
set DB_PASSWORD=nucosmos_dev_password
set JWT_SECRET_BASE64=bmV2ZXItc2hpcC10aGlzLWRlZmF1bHQtc2VjcmV0LWluLXByb2QtdXNlLWFuLWVudi12YXI=
set JWT_ACCESS_TOKEN_MINUTES=480
set SERVER_PORT=8081
set FRONTEND_ORIGIN=http://localhost:5173
echo DB_HOST=%DB_HOST%
echo DB_PORT=%DB_PORT%
echo SERVER_PORT=%SERVER_PORT%
echo FRONTEND_ORIGIN=%FRONTEND_ORIGIN%
call mvnw.cmd spring-boot:run
