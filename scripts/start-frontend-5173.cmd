@echo off
setlocal
cd /d C:\NUCOSMOS_POS
set VITE_API_BASE_URL=http://localhost:8081
echo VITE_API_BASE_URL=%VITE_API_BASE_URL%
call npm.cmd run dev -- --host 0.0.0.0 --port 5173
