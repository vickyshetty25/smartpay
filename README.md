# SmartPay — Payment & Wallet Microservices

A production-grade 5-service payment system built with Java 21, Spring Boot, Apache Kafka, Redis, and PostgreSQL. Each service owns its own database. Services communicate asynchronously via Kafka — never directly.

---

## Architecture
```
Client → API Gateway (8080) → JWT Validation → Rate Limiting → Route
                  ↓
    /api/users/**        → User Service (8081)
    /api/payments/**     → Payment Service (8082)
    /api/wallets/**      → Wallet Service (8083)
    /api/notifications/** → Notification Service (8084)

Payment Service → Kafka (payment.initiated) → Wallet Service
                                             → Notification Service
```

---

## The 5 Services

| Service | Port | Responsibility |
|---|---|---|
| API Gateway | 8080 | JWT validation, rate limiting, routing |
| User Service | 8081 | Register, login, JWT generation |
| Payment Service | 8082 | Create payments, Kafka producer, idempotency |
| Wallet Service | 8083 | Kafka consumer, balance deduction/credit |
| Notification Service | 8084 | Kafka consumer, payment alerts |

---

## Tech Stack

| Technology | Version | Usage |
|---|---|---|
| Java | 21 | All services |
| Spring Boot | 4.0.4 | All services |
| Apache Kafka | 4.1.2 | Async event streaming |
| Redis | 7 | Idempotency keys + rate limiting |
| PostgreSQL | 16 | Each service has own DB |
| JWT (jjwt) | 0.11.5 | Authentication |
| Docker + docker-compose | latest | One command setup |
| Maven | - | Build tool |

---

## Key Features

### 1. JWT Authentication at Gateway
Every request (except register/login) is validated at the API Gateway before reaching any service. Invalid tokens are rejected immediately — downstream services are never touched.

### 2. Redis Idempotency Keys
Client sends a UUID with every payment request. Payment Service checks Redis first:
- Key exists → return original payment, no duplicate processing
- Key missing → create payment, store in Redis with 24hr TTL
```
POST /api/payments/initiate
{
    "receiverId": 1,
    "amount": 500.00,
    "idempotencyKey": "unique-uuid-here"  ← same UUID = same payment
}
```

### 3. Kafka Event Streaming
Payment Service never calls Wallet or Notification Service directly. It publishes a PaymentEvent to Kafka. Both services consume independently:
- Wallet Service → deducts sender, credits receiver
- Notification Service → saves alerts for both users

If Notification Service goes down, payments still work. When it restarts, it replays all missed events from Kafka.

### 4. Redis Rate Limiting (Sliding Window)
API Gateway limits 10 requests per user per 60 seconds using Redis sorted sets. Returns HTTP 429 on breach. More accurate than fixed-window — no boundary exploitation.

### 5. Database Per Service
Each service has its own PostgreSQL database — no shared DB, no tight coupling at the data layer.

---

## API Endpoints (via Gateway on port 8080)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | /api/users/register | No | Register new user |
| POST | /api/users/login | No | Login, get JWT token |
| POST | /api/payments/initiate | Yes | Initiate payment |
| GET | /api/payments/history | Yes | Payment history |
| GET | /api/payments/{paymentId} | Yes | Get payment by ID |
| POST | /api/wallets/create | Yes | Create wallet |
| GET | /api/wallets/{userId} | Yes | Get wallet balance |
| GET | /api/notifications/{userId} | Yes | Get notifications |

---

## How to Run
```bash
git clone https://github.com/vickyshetty25/smartpay.git
cd smartpay
docker compose up --build
```

All 5 services start automatically with their own PostgreSQL databases.

---

## Sample Payment Flow

**1. Register:**
```json
POST /api/users/register
{ "name": "Vicky", "email": "vicky@gmail.com", "password": "pass123" }
```

**2. Login → get token:**
```json
POST /api/users/login
{ "email": "vicky@gmail.com", "password": "pass123" }
→ { "token": "eyJhbGci..." }
```

**3. Create wallet:**
```json
POST /api/wallets/create
{ "userId": 1, "initialBalance": 10000.00 }
```

**4. Send payment:**
```json
POST /api/payments/initiate
Authorization: Bearer {token}
{
    "receiverId": 2,
    "amount": 500.00,
    "description": "Lunch split",
    "idempotencyKey": "unique-uuid-001"
}
```

**5. Check notifications:**
```json
GET /api/notifications/1
Authorization: Bearer {token}
→ "Your payment of ₹500.00 was sent successfully."
```

---

## Interview Q&A

**Q: Why microservices instead of monolith?**
Each service deploys independently. Payment Service can scale to handle 10x traffic without scaling User Service. If Notification Service goes down, payments keep working.

**Q: How do you prevent duplicate payments?**
Idempotency keys stored in Redis with 24hr TTL. Client sends a UUID per request. Redis setIfAbsent returns false if key exists — duplicate gets original payment ID back.

**Q: What happens if Kafka is down?**
Payment Service still accepts payments and stores them in PostgreSQL. When Kafka recovers, events are replayed. For critical failures, a @Scheduled job can detect payments with no wallet debit and retry.

**Q: How does rate limiting work?**
Redis sorted set per user. Each request adds a timestamp. Timestamps older than 60 seconds are removed. If count >= 10, return 429. Sliding window means no gaming the boundary.