# Reward Backend Architecture Document

## 1. Overview

This project is a Spring Boot-based backend service for a smart rewards system. It manages businesses, users, wallets, reward rules, and transaction events. The application currently follows a modular monolith architecture with a layered structure: controller -> service -> repository -> database.

The system supports:
- Creating and tracking reward events such as purchase, signup, and referral
- Applying reward rules per business and event type
- Maintaining per-user, per-business wallet balances
- Storing transaction history for reward events
- Exposing REST endpoints for wallet and event processing

---

## 2. Business Context

The backend models a reward engine for businesses that want to offer points-based loyalty programs. Key domain concepts are:

- Business: a brand or merchant such as Taj, Club ABC
- User: customer or member
- RewardRule: logic for how points are granted based on business and event type
- Wallet: user balance within a specific business context
- Transaction: history of events and awarded points

The core idea is that each business defines rules, and user activities generate points that are stored in a wallet and tracked as transactions.

---

## 3. Architectural Style

### 3.1 Current Architecture

The project is implemented as a single Spring Boot application with the following layers:

1. API Layer
   - REST controllers in `src/main/kotlin/com/smartReward/backend/api`
   - Handles incoming HTTP requests

2. Service Layer
   - Domain logic sits in `service`
   - Main responsibilities:
     - processing events
     - returning wallet state

3. Rule Engine Layer
   - `RuleEngine` centralizes reward evaluation logic
   - Evaluates active rules for a given business and event

4. Persistence Layer
   - Repositories under `repository`
   - Spring Data JPA interfaces manage database access

5. Model Layer
   - JPA entities in `model`
   - Maps to relational tables

6. Configuration Layer
   - `config` contains bootstrapping and CORS configuration

This is a classic layered monolith pattern, appropriate for early-stage MVP or internal service work.

### 3.2 Communication Pattern

- HTTP requests come into controller endpoints
- Controllers delegate business logic to service classes
- Services interact with repositories for database persistence
- Data is persisted to a relational DB via JPA/Hibernate
- Rule engine calculates points based on configured rules

---

## 4. Technology Stack

### 4.1 Core Language and Runtime

- Kotlin 2.2.21
- Java 17
- Spring Boot 4.0.6

### 4.2 Framework and Libraries

- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Hibernate via JPA
- Jackson with Kotlin module for JSON serialization/deserialization
- Spring Boot DevTools

### 4.3 Database

- PostgreSQL as the production database dependency
- H2 in-memory database for local/test development

Current configuration shows H2 is active in `application.yml`, which is suitable for local development and testing, while PostgreSQL is included for production readiness.

### 4.4 Testing Stack

- Spring Boot starter test libraries
- Kotlin test for JUnit 5
- JUnit Platform

### 4.5 Build and Dependency Management

- Gradle Kotlin DSL (`build.gradle.kts`)
- Gradle wrapper included
- Spring Dependency Management Plugin

### 4.6 Tools and Utilities

- IntelliJ / VS Code compatible Kotlin project
- Git for source control
- CORS configuration included for browser-based clients

---

## 5. Build and Project Structure

### 5.1 Root Structure

```text
RewardBackend/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew
├─ gradlew.bat
├─ src/
│  ├─ main/
│  │  ├─ kotlin/com/smartReward/backend/
│  │  │  ├─ api/
│  │  │  ├─ config/
│  │  │  ├─ dto/
│  │  │  ├─ model/
│  │  │  ├─ repository/
│  │  │  ├─ ruleengine/
│  │  │  ├─ service/
│  │  │  └─ BackendApplication.kt
│  │  └─ resources/
│  │     ├─ application.properties
│  │     └─ application.yml
│  └─ test/
│     └─ kotlin/com/smartReward/backend/
└─ ARCHITECTURE.md
```

### 5.2 Key Package Responsibilities

- `api` : REST API endpoints
- `service` : business orchestration and rules execution
- `ruleengine` : reward logic evaluation
- `repository` : data access layer
- `model` : JPA entities
- `dto` : request/response contracts
- `config` : app config, data seeding, CORS

---

## 6. Runtime Components

### 6.1 Application Bootstrap

The application entry point is `BackendApplication.kt`:

- `@SpringBootApplication` starts the application context
- `runApplication<BackendApplication>(*args)` launches the backend
- A health controller is also present for basic status checks

A health endpoint exists at:
- `/api/health`
- `/health`
- `/businesses`

This indicates the service is intentionally kept lightweight and operational for testing/demo purposes.

### 6.2 Configuration

The app config currently includes:

- application name: `backend`
- H2 in-memory database enabled by default
- JPA Hibernate `ddl-auto: update`
- H2 console enabled
- CORS enabled globally for all origins

This setup is good for demo/dev work but not production-grade security configuration.

---

## 7. Domain Model

### 7.1 Business

Entity: `Business`

Fields:
- `id: String`
- `name: String`
- `createdAt: Long`

Table: `businesses`

Purpose:
- represents a merchant or company using the reward system

### 7.2 User

Entity: `User`

Fields:
- `id: String`
- `name: String?`
- `createdAt: Long`

Table: `users`

Purpose:
- tracks loyalty members or app users

### 7.3 RewardRule

Entity: `RewardRule`

Fields:
- `id: Long`
- `businessId: String`
- `eventType: String`
- `minAmount: Double?`
- `rewardType: String`
- `rewardValue: Double`
- `isActive: Boolean`
- `createdAt: Long`

Table: `reward_rules`

Purpose:
- declarative business logic for rewards
- supports rules like:
  - Taj purchase minimum 50, reward 10% of amount
  - Sign-up reward = 100 flat points
  - Referral reward = 250 flat points

### 7.4 Wallet

Entity: `Wallet`

Fields:
- `id: Long`
- `userId: String`
- `businessId: String`
- `availablePoints: Int`
- `pendingPoints: Int`
- `updatedAt: Long`

Unique constraint:
- `(userId, businessId)` must be unique

Purpose:
- tracks current wallet balance by user and brand

### 7.5 Transaction

Entity: `Transaction`

Fields:
- `id: Long`
- `userId: String`
- `businessId: String`
- `eventType: String`
- `points: Int`
- `status: String`
- `referenceId: String?`
- `createdAt: Long`

Table: `transactions`

Purpose:
- audit trail and ledger of point events

---

## 8. Persistence and Repository Design

Repositories are Spring Data JPA interfaces:

- `BusinessRepository`
- `RewardRuleRepository`
- `WalletRepository`
- `TransactionRepository`

### 8.1 RewardRuleRepository

Custom method:
- `findByBusinessIdAndEventTypeAndIsActiveTrue(businessId, eventType)`

This is central to rule matching during event processing.

### 8.2 WalletRepository

Custom method:
- `findByUserIdAndBusinessId(userId, businessId)`

This allows retrieving a user’s wallet in a specific business context.

### 8.3 TransactionRepository

General-purpose transaction persistence.

---

## 9. Core Business Flows

### 9.1 Event Processing Flow

The main flow is defined in `EventService.processEvent()`.

1. Receive an `EventRequest`
2. Determine effective event type (`eventType` or `event` alias)
3. Fetch active reward rules for the business and event type
4. Evaluate rules using the rule engine
5. If points are awarded:
   - create or fetch wallet
   - add awarded points to `pendingPoints`
   - create a `Transaction` with status `PENDING`
   - save both wallet and transaction
6. If no active rule matches:
   - return a successful event message with zero points

This is the system’s central orchestration path.

### 9.2 Rule Evaluation Flow

The `RuleEngine` does the following:

- filters rules to `isActive == true`
- checks `minAmount` threshold when present
- calculates reward amount based on `rewardType`

Supported reward types:
- `PERCENTAGE`
- `FLAT`

Example:
- percentage rule: `amount * rewardValue / 100`
- flat rule: fixed points award

### 9.3 Wallet Lookup Flow

`WalletController` exposes:
- `GET /wallet/{businessId}/{userId}`

The service returns a `WalletResponse` containing:
- available points
- pending points

---

## 10. API Layer

### 10.1 Event API

Controller: `EventController`

Endpoint:
- `POST /events`

Request DTO: `EventRequest`

Example properties:
- `businessId`
- `userId`
- `eventType` (supports alias `event` and `type` via `@JsonAlias`)
- `amount`
- `referenceId`
- `properties`

Response DTO: `EventResponse`

Response includes:
- `success`
- `pointsAwarded`
- `matchedRulesCount`
- `transactionId`
- `message`
- `pendingPoints`
- `availablePoints`

### 10.2 Wallet API

Controller: `WalletController`

Endpoint:
- `GET /wallet/{businessId}/{userId}`

This returns wallet state for a specific user in a specific business.

### 10.3 Health API

The main application also includes a health check controller.

Endpoints:
- `/api/health`
- `/health`
- `/businesses`

These are useful for smoke testing and boot verification.

---

## 11. Data Seeding

`DataInitializer.kt` seeds default reward rules if the table is empty.

Seed examples include:
- Taj purchase rule: minimum amount 50, 10% reward
- Taj signup bonus: 100 flat points
- Taj referral bonus: 250 flat points

This is a lightweight bootstrapping mechanism for development/demo setup.

---

## 12. Security and CORS

`WebCorsConfig.kt` adds a global `CorsFilter` that allows all origins, headers, and methods.

This is useful for quick front-end integration but should be tightened in production with:
- explicit allowed origins
- restricted methods
- environment-specific config
- authentication/authorization layer

There is no explicit security mechanism (JWT/Security filter, OAuth, RBAC) currently implemented.

---

## 13. Dependency and Tooling Notes

### 13.1 Build Tools

- Gradle Kotlin DSL
- Spring Boot plugin
- Kotlin plugin for Spring
- JPA plugin for Kotlin

### 13.2 Development Experience

- `spring-boot-devtools` for hot reload support
- H2 console accessible in dev mode
- lightweight local configuration for rapid iteration

---

## 14. Current Strengths

- Clear layered architecture for a small service
- Kotlin + Spring Boot is modern and developer-friendly
- JPA repositories reduce boilerplate
- Rule engine is cleanly separated from controller and persistence logic
- Easy to extend with more event types and rule types
- Seed data makes the app testable out of the box

---

## 15. Notable Gaps and Risks

This project is functional for prototype/demo use, but a few areas need attention before production readiness:

1. No authentication or authorization layer
2. No API versioning strategy
3. CORS is globally open to all origins
4. No explicit validation for required request fields beyond DTO defaults
5. Wallet and points logic may need stronger business rules around redemption, expiry, and settlement
6. `pendingPoints` and `availablePoints` are not yet clearly separated in real business logic
7. Rule engine currently uses `toInt()` conversions, which may truncate fractional values unexpectedly
8. The `WalletService.getWallet` parameter order is `userId, businessId`, while controller passes `(businessId, userId)` and calls via named arguments in the controller, which works but is confusing and may cause maintenance issues
9. `application.yml` is using H2 local config while PostgreSQL is a dependency; production DB configuration is not yet fully wired
10. `Business` / `User` entities are present but not yet strongly integrated with real user lifecycle operations

---

## 16. Recommended Architecture Evolution

If this project is expected to grow, the best next step is to evolve from a simple monolith into a more explicit domain-driven structure.

Suggested future direction:

- Introduce authentication and authorization service
- Add API gateway or auth proxy
- Split business logic into bounded contexts:
  - User management
  - Reward rule management
  - Wallet management
  - Event processing
  - Reporting/audit
- Add domain services for redemption, expiry, and settlements
- Add proper database migration tooling such as Flyway or Liquibase
- Move from in-memory/dev DB config to environment-based production configuration
- Add observability: logging, metrics, traces
- Add integration tests and contract tests

---

## 17. Summary

The Reward Backend is a lightweight Kotlin + Spring Boot application implementing a reward processing engine for businesses. It follows a layered monolithic design with clearly separated controllers, services, repositories, and JPA entities. The core capability is event-driven reward calculation based on business-specific rules, with wallet balances and transaction history stored in a relational database.

The project is a strong MVP foundation and is well-suited for internal demos and early-stage iteration, but it still needs production-oriented hardening around security, database configuration, validation, and operational concerns.

---

## 18. Relevant Project Files

- `build.gradle.kts`
- `src/main/kotlin/com/smartReward/backend/BackendApplication.kt`
- `src/main/kotlin/com/smartReward/backend/api/EventController.kt`
- `src/main/kotlin/com/smartReward/backend/api/WalletController.kt`
- `src/main/kotlin/com/smartReward/backend/service/EventService.kt`
- `src/main/kotlin/com/smartReward/backend/service/WalletService.kt`
- `src/main/kotlin/com/smartReward/backend/ruleengine/RuleEngine.kt`
- `src/main/kotlin/com/smartReward/backend/model/*.kt`
- `src/main/kotlin/com/smartReward/backend/repository/*.kt`
- `src/main/resources/application.yml`
