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

## Tenant and Branch Model

Reward uses one tenant and one PostgreSQL database for a business group. Branches
are logical units under that tenant and are stored in `reward_branches`. Members
and reward accounts remain tenant-wide, so a member keeps one identity and one
shared points balance across branches.

Branch-specific rules are stored in `reward_branch_rules`. A rule with a
`branchId` applies only to that branch; a rule without one is tenant-wide. Event
requests may include `branchCode`, and the resolved branch is recorded on
transactions and wallet history for reporting.

Example branch:

```bash
curl -X POST http://localhost:8080/api/branches \
	-H "Content-Type: application/json" \
	-d '{
		"tenantId": "hotel-group",
		"code": "MUM-01",
		"name": "Mumbai Central",
		"city": "Mumbai"
	}'
```

Example branch-scoped rule:

```bash
curl -X POST http://localhost:8080/api/rules \
	-H "Content-Type: application/json" \
	-d '{
		"tenantId": "hotel-group",
		"branchId": "<branch-id>",
		"name": "Mumbai premium earn rate",
		"eventType": "PURCHASE",
		"rewardType": "PERCENTAGE",
		"rewardValue": 15
	}'
```

Runtime events use the branch code while the member remains shared:

```json
{
	"tenantId": "hotel-group",
	"branchCode": "MUM-01",
	"memberId": "member-external-id",
	"eventType": "PURCHASE",
	"amount": 5000
}
```

## Tenant Provisioning

Provisioning creates one business tenant in the shared PostgreSQL database. It
returns a tenant-specific base URL, logical schema name, one-time API key,
global program, and default tiers. The API key is stored only as a SHA-256 hash
and must be sent as `X-API-Key` on subsequent API calls.

```bash
curl -X POST http://localhost:8080/api/provisioning/tenants \
	-H "Content-Type: application/json" \
	-d '{
		"name": "Indian Hotel Group",
		"slug": "indianhotel",
		"adminEmail": "admin@indianhotel.com",
		"programName": "Indian Hotel Rewards",
		"currency": "INR",
		"earningRate": 10,
		"redemptionRate": 1,
		"tiers": [
			{"name": "SILVER", "rank": 1, "thresholdPoints": 0, "multiplier": 1},
			{"name": "GOLD", "rank": 2, "thresholdPoints": 1000, "multiplier": 1.25},
			{"name": "PLATINUM", "rank": 3, "thresholdPoints": 5000, "multiplier": 1.5}
		]
	}'
```

The default URL is `https://indhotel.benevo.io`; set
`PLATFORM_BASE_DOMAIN` for your deployed domain. `schemaName` is stored as a
tenant namespace while the current implementation uses shared PostgreSQL
tables with tenant IDs, which avoids one physical table set per business and
scales better for many tenants. Tenant-specific rows must always be filtered
by `tenantId`.
