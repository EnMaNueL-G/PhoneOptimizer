@echo off
chcp 65001 >nul
color 0A
title PhoneOptimizer — Activar Optimización Avanzada

echo.
echo  ╔══════════════════════════════════════════════════════════╗
echo  ║        PhoneOptimizer — Activación Avanzada             ║
echo  ║     Este programa activa las funciones premium          ║
echo  ║     de PhoneOptimizer en tu Android. Solo necesitas     ║
echo  ║     conectar tu teléfono y hacer clic en Aceptar.       ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.

:: ── Buscar ADB ──────────────────────────────────────────────────────────────
set ADB=
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
    goto :found_adb
)
if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set ADB=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe
    goto :found_adb
)
where adb >nul 2>&1
if %ERRORLEVEL%==0 (
    set ADB=adb
    goto :found_adb
)

:: ADB no encontrado — descargarlo automáticamente
echo  [!] ADB no está instalado. Descargando automáticamente...
echo.
powershell -Command "& { $url='https://dl.google.com/android/repository/platform-tools-latest-windows.zip'; $zip='%TEMP%\platform-tools.zip'; $dst='%TEMP%\pt'; Invoke-WebRequest $url -OutFile $zip -UseBasicParsing; Expand-Archive $zip $dst -Force; Write-Host 'Descarga completa.' }"
if exist "%TEMP%\pt\platform-tools\adb.exe" (
    set ADB=%TEMP%\pt\platform-tools\adb.exe
    echo  [✓] ADB descargado correctamente
    goto :found_adb
) else (
    color 0C
    echo  [ERROR] No se pudo descargar ADB. Verifica tu conexión a internet.
    pause
    exit /b 1
)

:found_adb
echo  [✓] ADB encontrado: %ADB%
echo.

:: ── Verificar teléfono conectado ─────────────────────────────────────────────
echo  Paso 1 — Conecta tu teléfono al PC con el cable USB
echo  Paso 2 — Si aparece un mensaje en el teléfono "Permitir depuración USB" → toca PERMITIR
echo.
echo  Esperando teléfono...
echo.

:wait_device
"%ADB%" kill-server >nul 2>&1
"%ADB%" start-server >nul 2>&1
for /f "tokens=1" %%i in ('"%ADB%" devices ^| findstr /v "List" ^| findstr "device"') do set DEVICE_ID=%%i

if "%DEVICE_ID%"=="" (
    timeout /t 3 /nobreak >nul
    goto :wait_device
)

echo  [✓] Teléfono detectado: %DEVICE_ID%
echo.

:: ── Verificar si es MIUI V14 ────────────────────────────────────────────────
for /f "tokens=*" %%v in ('"%ADB%" -s %DEVICE_ID% shell getprop ro.miui.ui.version.name 2^>nul') do set MIUI_VER=%%v
for /f "tokens=*" %%m in ('"%ADB%" -s %DEVICE_ID% shell getprop ro.product.model 2^>nul') do set MODEL=%%m

echo  Modelo: %MODEL%
if defined MIUI_VER echo  MIUI: %MIUI_VER%
echo.

if "%MIUI_VER%"=="V140" goto :miui_blocked
if "%MIUI_VER%"=="V150" goto :miui_blocked
if "%MIUI_VER%"=="V160" goto :miui_blocked

:: ── Ejecutar el comando ──────────────────────────────────────────────────────
echo  Activando funciones avanzadas...
"%ADB%" -s %DEVICE_ID% shell pm grant com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS
if %ERRORLEVEL%==0 (
    color 0A
    echo.
    echo  ╔══════════════════════════════════════════════════════════╗
    echo  ║   ✓ ACTIVADO CORRECTAMENTE                              ║
    echo  ║                                                          ║
    echo  ║   Ahora en tu teléfono:                                 ║
    echo  ║   1. Cierra PhoneOptimizer completamente                ║
    echo  ║   2. Vuelve a abrirla                                    ║
    echo  ║   3. Verás "Optimización completa activa" en verde      ║
    echo  ╚══════════════════════════════════════════════════════════╝
) else (
    color 0E
    echo.
    echo  [!] El permiso no pudo activarse en este dispositivo.
    echo  [!] Puede deberse a restricciones del fabricante (Xiaomi MIUI V14).
    echo  [!] Las funciones básicas de la app siguen funcionando.
)
echo.
pause
exit /b 0

:miui_blocked
color 0E
echo  ╔══════════════════════════════════════════════════════════╗
echo  ║   ⚠ XIAOMI MIUI V14 / HyperOS DETECTADO                ║
echo  ║                                                          ║
echo  ║   Tu teléfono bloquea este tipo de activación.         ║
echo  ║   NO es un error de PhoneOptimizer.                     ║
echo  ║                                                          ║
echo  ║   La app funciona en modo básico:                       ║
echo  ║   ✓ Libera RAM                                          ║
echo  ║   ✓ Detiene procesos                                    ║
echo  ║   ✓ Garbage Collection                                  ║
echo  ║                                                          ║
echo  ║   Funciones no disponibles en MIUI V14:                 ║
echo  ║   ✗ Animaciones reducidas                               ║
echo  ║   ✗ WiFi scan                                           ║
echo  ║   ✗ Doze profundo                                       ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.
pause
exit /b 0
