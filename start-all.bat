@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

echo 🧪 Starting simulation with UTF-8 support...
echo.
<nul set /p="Inicializando... (forçando reset de STDIN)"

echo.
echo ================================
echo Compilando todos os módulos
echo ================================

call mvn clean install -DskipTests -pl order-service,pricing-service,audit-service,notification-service,external-cotation-gw,log-collector-service

if %ERRORLEVEL% NEQ 0 (
    echo Erro na compilação. Abortando.
    pause
    exit /b %ERRORLEVEL%
)

set "LOG_COLLECTOR_URL=http://localhost:8090/log"

echo.
echo ================================
echo Iniciando Log Collector Service
echo ================================

start "Log Collector Service" cmd /k "chcp 65001 > nul && cd log-collector-service && mvn spring-boot:run"

REM === Aguarda até que o Log Collector esteja respondendo ===
echo.
echo Aguardando Log Collector ficar disponível...

:WAIT_LOOP
for /f "tokens=*" %%i in ('curl -s -o nul -w "%%{http_code}" http://localhost:8090/actuator/health') do (
    if "%%i" NEQ "200" (
        timeout /t 1 >nul
        goto WAIT_LOOP
    )
)

echo Log Collector disponível. Prosseguindo...

echo.
echo ================================
echo Iniciando os demais microsserviços
echo ================================

@echo on
set "ORDER_ARGS=-Dspring-boot.run.jvmArguments=-Dlog.collector.url=%LOG_COLLECTOR_URL% -Dlog.service.name=OrderService"
set "PRICING_ARGS=-Dspring-boot.run.jvmArguments=-Dlog.collector.url=%LOG_COLLECTOR_URL% -Dlog.service.name=PricingService"
set "AUDIT_ARGS=-Dspring-boot.run.jvmArguments=-Dlog.collector.url=%LOG_COLLECTOR_URL% -Dlog.service.name=AuditService"
set "NOTIF_ARGS=-Dspring-boot.run.jvmArguments=-Dlog.collector.url=%LOG_COLLECTOR_URL% -Dlog.service.name=NotificationService"
set "GW_ARGS=-Dspring-boot.run.jvmArguments=-Dlog.collector.url=%LOG_COLLECTOR_URL% -Dlog.service.name=ExternalCotationGateway"

start "Order Service" cmd /k "chcp 65001 > nul && cd order-service && mvn spring-boot:run"
start "Pricing Service" cmd /k "chcp 65001 > nul && cd pricing-service && mvn spring-boot:run"
start "Audit Service" cmd /k "chcp 65001 > nul && cd audit-service && mvn spring-boot:run"
start "Notification Service" cmd /k "chcp 65001 > nul && cd notification-service && mvn spring-boot:run"
start "External Cotation Gateway" cmd /k "chcp 65001 > nul && cd external-cotation-gw && mvn spring-boot:run"
@echo off

echo.
echo ================================
echo Todos os microsserviços foram iniciados.
echo ================================

echo.
choice /M "Deseja iniciar o Integration Simulator agora?"

if %ERRORLEVEL% EQU 1 (
    echo Iniciando o Integration Simulator...
    start "Integration Simulator" cmd /k "chcp 65001 > nul && cd integration-sim && mvn exec:java -Dexec.mainClass=com.energytrade.integrationsim.IntegrationSimulator"
) else (
    echo Simulador não iniciado.
)

pause
