# Wallet Service

Java 21 / Spring Boot backend for wallet management, fund transfers, and transaction history.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Data JPA
- PostgreSQL (local) via `application.properties`
- Bean Validation
- Lombok

## Project layout

```
src/main/java/com/wallet/
  WalletApplication.java
  controller/     REST endpoints
  dto/            Request / response models
  entity/         User, Wallet, TxnHistory
  enums/          TxnType, TxnStatus
  exception/      Business errors + global handler
  repository/     JPA repositories (pessimistic lock on wallet)
  service/        WalletService (create, top-up, withdraw, transfer, history)
src/main/resources/
  application.properties
```

## Domain rules

| Rule | Enforcement |
|------|-------------|
| User 1 — 1 Wallet | Unique `user_id` on wallet; cascade create |
| `balance >= 0` | Entity invariant + debit guard |
| `maxBalance > 0` | Entity invariant |
| `balance <= maxBalance` | Entity invariant + credit guard |
| Concurrent transfers | `PESSIMISTIC_WRITE` + lock wallets in ascending id order (avoids A↔C deadlock) |
| Failed ops recorded | `txn_history` stores SUCCESS and FAILURE |

## Run locally

```bash
# from project root
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

- API base: `http://localhost:8080/api`
- DB: local PostgreSQL `jdbc:postgresql://localhost:5432/ashishranjan` (user `ashishranjan`)

Ensure Postgres is running locally, e.g. `psql -U ashishranjan -d ashishranjan`. Hibernate `ddl-auto=update` creates tables on startup.

## API

### Create user + wallet

`POST /api/create_user`

```json
{
  "name": "Ashish",
  "email": "ashish@example.com",
  "phone": "9999999999",
  "address": "Bangalore",
  "maxBalance": 50000.00
}
```

Response `201`: `{ "userId": 1, "walletId": 1, "message": "..." }`  
Error: `409 USER_ALREADY_REGISTERED`

### Balance

`GET /api/wallet_balance/{walletId}` → `{ "walletId", "amount", "maxAmount" }`

### Top-up

`POST /api/top_up`

```json
{ "walletId": 1, "amount": 1000.00 }
```

### Withdraw

`POST /api/withdraw`

```json
{ "walletId": 1, "externalId": "BANK-ACC-1", "amount": 200.00 }
```

### Transfer

`POST /api/transfer`

```json
{ "fromId": 1, "toId": 2, "amount": 150.00 }
```

### Transaction history

`GET /api/txn_details?walletId=1&startDate=2024-01-01T00:00:00Z&endDate=2026-12-31T23:59:59Z&txnType=TRANSFER&page=0&size=20`

`txnType`: `WITHDRAW` | `TOP_UP` | `TRANSFER`

## Postman

Import from the `postman/` folder:

1. **Collection:** `postman/Wallet_Service.postman_collection.json`
2. **Environment (optional):** `postman/Wallet_Service.local.postman_environment.json`

In Postman: **Import → Upload Files** → select both. Set environment to **Wallet Service - Local** (or use collection variables; `baseUrl` defaults to `http://localhost:8080`).

Folders:

| Folder | Contents |
|--------|----------|
| 1. Users | Create user A/B, duplicate email, validation |
| 2. Wallet Balance | Get balance, not found |
| 3. Top Up | Success + max-balance / invalid amount |
| 4. Withdraw | Success + insufficient balance |
| 5. Transfer | A→B, B→A, same-wallet, insufficient |
| 6. Transaction History | Filters by type, date range, pagination |
| 7. Happy Path Runner | Ordered E2E flow (unique emails via `{{$timestamp}}`) |

Create User A/B scripts auto-save `walletId` / `walletId2` into collection variables.

## Sample curl flow

```bash
# 1. Create two users
curl -s -X POST http://localhost:8080/api/create_user \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","phone":"111","address":"A"}'

curl -s -X POST http://localhost:8080/api/create_user \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bob","email":"bob@example.com","phone":"222","address":"B"}'

# 2. Top-up Alice (walletId from step 1)
curl -s -X POST http://localhost:8080/api/top_up \
  -H 'Content-Type: application/json' \
  -d '{"walletId":1,"amount":5000}'

# 3. Transfer Alice -> Bob
curl -s -X POST http://localhost:8080/api/transfer \
  -H 'Content-Type: application/json' \
  -d '{"fromId":1,"toId":2,"amount":1200}'

# 4. History
curl -s 'http://localhost:8080/api/txn_details?walletId=1&page=0&size=10'
```
