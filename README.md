# Patient Management Microservices

A microservices-based backend system built with Java Spring Boot, PostgreSQL, Kafka, and gRPC.

---

## Services

| Service | Port | Description |
|---|---|---|
| `api-gateway` | 4004 | Entry point for all client requests, validates JWT tokens |
| `auth-service` | 4005 | Handles login and JWT token generation/validation |
| `patient-service` | 4000 | Manages patient CRUD operations |
| `billing-service` | 4001 / 9001 | Billing logic, exposes gRPC on port 9001 |
| `analytics-service` | 4002 | Consumes Kafka events for analytics |
| `patient-service-db` | 5000 | PostgreSQL database for patient service |
| `auth-service-db` | 5001 | PostgreSQL database for auth service |
| `kafka` | 9092 / 9094 | Apache Kafka message broker |

---

## Prerequisites

- [Docker](https://www.docker.com/products/docker-desktop)
- [Docker Compose](https://docs.docker.com/compose/)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/AdnanJami/Patient-service.git
cd Patient-service
```

### 2. Run all services

```bash
docker-compose up --build -d
```

### 3. Stop all services

```bash
docker-compose down
```

---

## Authentication

All requests (except `/login`) must include a valid JWT token in the `Authorization` header.

### Login

**POST** `http://localhost:4004/login`

```json
{
    "email": "testuser@test.com",
    "password": "password123"
}
```

**Response:**

```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Validate Token

**GET** `http://localhost:4004/validate`

| Header | Value |
|---|---|
| `Authorization` | `Bearer <your_token>` |

---

## Patient API

Base URL: `http://localhost:4004`

### Get All Patients

**GET** `/patients`

### Get Patient by ID

**GET** `/patients/{id}`

### Create Patient

**POST** `/patients`

```json
{
    "name": "John Doe",
    "email": "john@example.com",
    "address": "123 Main St",
    "dateOfBirth": "1990-01-01",
    "registeredDate": "2024-01-01"
}
```

### Update Patient

**PUT** `/patients/{id}`

```json
{
    "name": "John Doe Updated",
    "email": "john@example.com",
    "address": "456 New St",
    "dateOfBirth": "1990-01-01"
}
```

### Patch Patient

**PATCH** `/patients/{id}`

```json
{
    "name": "John Doe Patched"
}
```

### Delete Patient

**DELETE** `/patients/{id}`

---

## Architecture

```
Client
  │
  ▼
api-gateway (4004)
  │
  ├──► auth-service (4005) ──► auth-service-db (5001)
  │
  └──► patient-service (4000) ──► patient-service-db (5000)
            │
            ├──► billing-service (4001) via gRPC (9001)
            │
            └──► kafka (9092) ──► analytics-service (4002)
```

---

## Environment Variables

Sensitive values should be stored in a `.env` file (not committed to git).

| Variable | Description |
|---|---|
| `JWT_SECRET` | Secret key for JWT signing |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `SPRING_DATASOURCE_USERNAME` | Database username |

---

## Notes

- The `patient-service-db` and `auth-service-db` data folders are mounted as volumes and persist between restarts.
- Services will wait for their dependencies (databases, Kafka) to be healthy before starting.
