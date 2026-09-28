@echo off
chcp 65001 >nul
title PhoneOptimizer - Activar modo avanzado
color 0A

echo.
echo  ============================================================
echo    PhoneOptimizer - Activar modo avanzado (una sola vez)
echo  ============================================================
echo.
echo   Concede a PhoneOptimizer el permiso para cambiar la velocidad
echo   de las animaciones, la busqueda WiFi/Bluetooth y el DNS para
echo   bloquear anuncios. Todo se puede restaurar desde la app.
echo   Nada mas: no instala nada en el telefono ni borra datos.
echo.

:: -- Buscar ADB --------------------------------------------------------------
set "ADB="
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB if exist "%~dp0platform-tools\adb.exe" set "ADB=%~dp0platform-tools\adb.exe"
if not defined ADB (
    where adb >nul 2>&1 && set "ADB=adb"
)
if defined ADB goto :found_adb

echo  [!] No se encontro ADB. Se descargara de Google (dl.google.com, oficial).
choice /c SN /m "  Descargar ahora"
if errorlevel 2 exit /b 1
powershell -NoProfile -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:TEMP 'platform-tools.zip'; Invoke-WebRequest 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip -UseBasicParsing; Expand-Archive $zip '%~dp0' -Force"
if exist "%~dp0platform-tools\adb.exe" (
    set "ADB=%~dp0platform-tools\adb.exe"
    goto :found_adb
)
color 0C
echo  [ERROR] No se pudo descargar ADB. Revisa la conexion a internet.
pause
exit /b 1

:found_adb
echo  [OK] ADB: %ADB%
echo.

:: -- Esperar al telefono -----------------------------------------------------
echo  1. En el telefono: Ajustes ^> Acerca del telefono ^> toca 7 veces
echo     "Numero de compilacion" para activar las Opciones de desarrollador.
echo  2. En Opciones de desarrollador activa "Depuracion USB".
echo     Xiaomi / Redmi / POCO: activa tambien
echo     "Depuracion USB (ajustes de seguridad)".
echo  3. Conecta el cable y, si el telefono lo pregunta, toca PERMITIR.
echo.
echo  Esperando telefono...
"%ADB%" start-server >nul 2>&1

:wait_device
set "DEVICE_ID="
set /a COUNT=0
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" (
        set /a COUNT+=1
        set "DEVICE_ID=%%a"
    )
)
if %COUNT%==0 (
    timeout /t 3 /nobreak >nul
    goto :wait_device
)
:: Varios telefonos conectados: preguntar cual (o usar ANDROID_SERIAL si esta definido)
if defined ANDROID_SERIAL set "DEVICE_ID=%ANDROID_SERIAL%" & goto :have_device
if %COUNT% GTR 1 (
    echo.
    echo  Hay %COUNT% telefonos conectados:
    "%ADB%" devices -l | findstr /v "List"
    set /p "DEVICE_ID=  Escribe el numero de serie (primera columna) del telefono a activar: "
)
:have_device

for /f "tokens=*" %%m in ('"%ADB%" -s %DEVICE_ID% shell getprop ro.product.model 2^>nul') do set "MODEL=%%m"
echo  [OK] Telefono: %MODEL% (%DEVICE_ID%)
echo.

:: -- Comprobar que la app esta instalada -------------------------------------
"%ADB%" -s %DEVICE_ID% shell pm path com.enmanuelgil.optimizer >nul 2>&1
if errorlevel 1 (
    color 0E
    echo  [!] PhoneOptimizer no esta instalada en este telefono. Instalala primero.
    pause
    exit /b 1
)

:: -- Conceder el permiso -----------------------------------------------------
echo  Activando...
"%ADB%" -s %DEVICE_ID% shell pm grant com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS

:: Verificar de verdad que quedo concedido
"%ADB%" -s %DEVICE_ID% shell dumpsys package com.enmanuelgil.optimizer | findstr /c:"WRITE_SECURE_SETTINGS: granted=true" >nul
if errorlevel 1 goto :failed

color 0A
echo.
echo  ============================================================
echo    [OK] MODO AVANZADO ACTIVADO
echo.
echo    Abre PhoneOptimizer: en Optimizar vera "Modo avanzado activo".
echo    Ya puedes desconectar el cable y desactivar la depuracion USB.
echo.
echo    Para quitarlo en el futuro:
echo    adb shell pm revoke com.enmanuelgil.optimizer android.permission.WRITE_SECURE_SETTINGS
echo  ============================================================
echo.
pause
exit /b 0

:failed
color 0E
echo.
echo  [!] El telefono no acepto el permiso.
echo      - Xiaomi / Redmi / POCO: activa "Depuracion USB (ajustes de seguridad)"
echo        en Opciones de desarrollador (puede pedir iniciar sesion en Mi Account)
echo        y vuelve a ejecutar este archivo.
echo      - Otras marcas: desconecta y conecta el cable y vuelve a intentarlo.
echo.
echo  La app sigue funcionando sin esto (diagnostico, consejos, apps mas usadas).
echo.
pause
exit /b 1
