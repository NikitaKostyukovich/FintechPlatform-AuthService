# Auth Service

Authentication and authorization microservice built with Java and Spring Boot. Handles user registration, login, token refresh, and JWT signing using RSA-256 with a JWKS endpoint for downstream services.

## Prerequisites

- Java 21+ (or the version configured in `pom.xml`)
- Maven 3.9+
- Docker & Docker Compose
- OpenSSL

## Local Setup

### 1. Configure Environment Variables

Copy the example environment file and update the values if needed:

```bash
cp .env.example .env
```

Key variables used by the application:
- `DB_NAME`, `DB_URL`, `DB_LOGIN`, `DB_PASSWORD` — PostgreSQL connection settings
- `JWT_ACCESS`, `JWT_REFRESH` — access and refresh token lifetimes (in milliseconds)
- `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH` — paths to the RSA key files
- `USER_SERVICE_URL` — base URL of the User Service

### 2. Generate RSA Key Pair

The service signs JWTs with an RSA-256 private key (PKCS#8 format) and verifies them / exposes JWKS using the corresponding public key (X.509 format).

Generate the keys before the first startup:

```bash
mkdir -p keys
openssl genpkey -algorithm RSA -out keys/private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in keys/private_key.pem -out keys/public_key.pem
```

> **Note:** `keys/private_key.pem` is gitignored and must never be committed to version control.

### 3. Run the Application

Using Docker Compose:

```bash
docker-compose up --build
```

Or locally with Maven (requires a running PostgreSQL instance):

```bash
./mvnw spring-boot:run
```

## Running Tests

Integration tests use Testcontainers (PostgreSQL), so make sure Docker is running:

```bash
./mvnw test
```