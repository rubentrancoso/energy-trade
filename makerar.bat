@echo off
chcp 65001 > nul
setlocal

set ARCHIVE_NAME=energy-trade-sim-full.rar
set RAR_PATH="C:\Program Files\WinRAR\rar.exe"

echo.
echo [1/3] Limpando projeto com Maven...
call mvn clean

if %errorlevel% neq 0 (
    echo Maven clean falhou. Abortando.
    exit /b %errorlevel%
)

echo.
echo [2/3] Limpando .rar antigo (se houver)...
if exist %ARCHIVE_NAME% (
    del /f /q %ARCHIVE_NAME%
)

echo.
echo [3/3] Gerando novo arquivo %ARCHIVE_NAME%...

:: Verifica se rar.exe está disponível
if not exist %RAR_PATH% (
    echo ERRO: rar.exe não encontrado em %RAR_PATH%
    exit /b 1
)

:: Cria o RAR ignorando:
:: - diretórios .git e .settings
:: - o próprio arquivo .rar (evita auto-inclusão)
%RAR_PATH% a -m0 -r -x*.git\* -x*.settings\* -x%ARCHIVE_NAME% %ARCHIVE_NAME% *

if %errorlevel% neq 0 (
    echo Falha ao criar o arquivo RAR.
    exit /b %errorlevel%
)

echo.
echo ✅ Arquivo criado com sucesso: %ARCHIVE_NAME%
endlocal
pause
