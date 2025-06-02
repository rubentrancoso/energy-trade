# Energy Trade Simulator

Energy Trade Simulator is a modular Java project designed to simulate a simplified energy trading ecosystem using microservices, REST communication, and event-driven logging.

The project is built with **Spring Boot**, **Undertow**, and **Log4j2**, aiming to demonstrate real-world service orchestration, observability, and maintainability in a distributed architecture.

## 🔧 Objectives

- Simulate a realistic trading lifecycle across loosely coupled services.
- Enable real-time observability via a centralized log collector.
- Serve as a blueprint for multithreaded, resilient, and modular system design.
- Showcase code readability, testability, and configuration best practices.

## 🧱 Microservices Overview

- `order-service`: Handles client trade requests and order emission.
- `pricing-service`: Simulates price retrieval or external price integration.
- `audit-service`: Persists events related to transactions and system actions.
- `notification-service`: Sends external communications (e.g., email, webhook).
- `external-cotation-gw`: Acts as a stubbed external pricing API.
- `log-collector-service`: Captures and centralizes logs from all services.

## 📦 Stack

- **Java 11**
- **Spring Boot 2.7**
- **Undertow** (replaces default Tomcat)
- **Log4j2** (with custom HTTP appender)
- **H2** (in-memory DB)
- **Maven**
- **Windows batch scripting** (for orchestration)

## 📡 Architecture Highlights

- Custom `HttpLogCollectorAppender` for JSON-based centralized logging
- Environment-controlled boot process with service readiness detection
- Health-check loop to ensure `log-collector-service` starts before others
- Each service reports structured logs enriched with `serviceName` metadata

## 🧪 Next Steps

1. ✅ Boot orchestration with log readiness checks
2. ✅ Centralized log collection with custom Log4j2 appender
3. ✅ Real-time log visibility and service attribution
4. 🧪 Simulate business operations via `integration-sim`
5. 🧪 Persist business-relevant audit logs to database
6. 🧪 Implement error propagation, retries and fallbacks
7. 🧪 Add tests and validation (unit, integration)
8. 🔍 CLI or dashboard for monitoring/log parsing
9. 📈 Performance and stress simulation

## 📁 Repository Structure
```text
energy-trade-sim/
├── order-service/
├── pricing-service/
├── audit-service/
├── notification-service/
├── external-cotation-gw/
├── log-collector-service/
├── integration-sim/
└── common-logging/
```

## ⚖ License

This project is licensed under the MIT License. See `LICENSE` file for details.


