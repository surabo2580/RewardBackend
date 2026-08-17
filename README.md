# Reward Platform

Reward is a scalable multi-tenant loyalty platform designed for multiple businesses to enroll into reward programs.

## Architecture Overview

The platform is organized as a modular system:

- `reward-core` — shared domain model and business concepts
- `reward-engine` — transaction processing and rule evaluation
- `reward-events` — event dispatch and integration hooks
- `reward-reporting` — reporting and analytics capabilities
- `reward-api` — public REST API and application entry point

## Design Principles

- multi-tenant by design
- clear separation of core domain and runtime services
- extensible rules and reward logic
- scalable service boundaries for future growth
- modular deployment path

## Current State

This repository has been migrated from a single Spring Boot monolith toward a modular architecture. The default structure is ready for extension into a full platform with:

- tenant provisioning
- multi-business program configuration
- member management
- wallet/account calculations
- transaction processing
- event-driven notifications
- reporting dashboards

## Example Tenancy Flow

1. A business registers as a tenant.
2. A program is created for that tenant.
3. Members are onboarded.
4. Reward rules are configured.
5. Transactions are processed by the reward engine.
6. Events are emitted to downstream consumers.
7. Reports are generated from analytics services.

## Planned Extensions

- REST APIs for tenant and program management
- persistence layer with PostgreSQL
- event bus for notifications and webhooks
- role-based program management
- AI/segmentation layer for recommendations
- dashboard and analytics integration

## Run

From the project root:

```bash
./gradlew :reward-api:bootRun
```
