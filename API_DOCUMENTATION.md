# Stock Portfolio Tracker - API Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Auth Service](#auth-service)
3. [Tracker Service](#tracker-service)
4. [Data Models](#data-models)
5. [Security](#security)
6. [Error Handling](#error-handling)

---

## Architecture Overview

### System Design
The application follows a **microservices architecture** with two main services:

```
┌─────────────────┐         ┌──────────────────┐
│   Mobile App    │────────▶│   Auth Service   │
│   (React Native)│         │   (Port: 8081)   │
└─────────────────┘         └──────────────────┘
         │                           │
         │                           ▼
         │                  ┌─────────────────┐
         └─────────────────▶│ Tracker Service │
                            │  (Port: 8082)   │
                            └─────────────────┘
                                     │
                                     ▼
                            ┌─────────────────┐
                            │    MongoDB      │
                            └─────────────────┘
```

### Technology Stack
- **Backend**: Spring Boot 3.x, Java 17+
- **Database**: MongoDB (NoSQL)
- **Security**: JWT (JSON Web Tokens), BCrypt password hashing
- **External APIs**: Yahoo Finance API
- **Frontend**: React Native (Expo)

### Key Design Patterns
1. **Service Layer Pattern**: Business logic separated from controllers
2. **Repository Pattern**: Data access abstraction
3. **DTO Pattern**: Data transfer between layers
4. **Dependency Injection**: Spring's @RequiredArgsConstructor with Lombok
5. **Exception Handling**: Global exception handler with custom exceptions

---

## Auth Service

**Base URL**: `http://localhost:8081`

### Purpose
Handles user authentication, authorization, and account management.

### Endpoints

#### 1. User Registration
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "phoneNumber": "+919876543210"  // Optional
}
```

**Response** (201 Created):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "USR_abc123",
  "expiresIn": 86400000
}
```

**Business Logic**:
- Validates email/phone uniqueness
- Generates unique userId using `UserIdGenerator`
- Hashes password with BCrypt
- Creates JWT access token (24h expiry) and refresh token (30d expiry)
- Stores refresh token in database
- Assigns ROLE_USER by default

**Validation Rules**:
- Email OR phone number required
- Password minimum 6 characters
- Email must be unique
- Phone number must be unique

---

#### 2. User Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "USR_abc123",
  "expiresIn": 86400000
}
```

**Business Logic**:
- Finds user by email or phone
- Verifies password using BCrypt
- Checks user status (must be ACTIVE)
- Updates lastLogin timestamp
- Generates new JWT tokens
- Invalidates old refresh tokens

---

#### 3. Google OAuth Login
```http
POST /api/auth/google
Content-Type: application/json

{
  "idToken": "google_id_token_here"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "USR_abc123",
  "expiresIn": 86400000
}
```

**Business Logic**:
- Verifies Google ID token using Google API client
- Extracts email from token payload
- If user exists with GOOGLE provider → login
- If user exists with PASSWORD provider → reject (must use password login)
- If new user → auto-register with GOOGLE provider
- No password stored for Google users

---

#### 4. Refresh Access Token
```http
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "userId": "USR_abc123",
  "expiresIn": 86400000
}
```

**Business Logic**:
- Validates refresh token exists and not revoked
- Checks expiry time
- Generates new access token
- Returns same refresh token (not rotated)

---

#### 5. Forgot Password - Send OTP
```http
POST /api/auth/forgot-password/send-otp
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response** (200 OK):
```json
"OTP sent to your email successfully"
```

**Business Logic**:
- Validates email exists in system
- Checks for recent OTP requests (rate limiting: 5 min cooldown)
- Generates 6-digit OTP using `OtpUtil`
- Stores OTP with 5-minute expiry
- Sends email via `EmailService`

---

#### 6. Forgot Password - Verify OTP & Reset
```http
POST /api/auth/forgot-password/verify-otp
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "newpassword123"
}
```

**Response** (200 OK):
```json
"Password reset successfully"
```

**Business Logic**:
- Validates OTP matches and not used
- Checks OTP not expired
- Hashes new password with BCrypt
- Updates user password
- Marks OTP as used

---

#### 7. Health Check
```http
GET /health
```

**Response** (200 OK):
```json
"Auth service is up"
```

---

### Auth Service Data Models

#### User Entity
```java
{
  "userId": "USR_abc123",           // Unique identifier
  "email": "user@example.com",
  "phoneNumber": "+919876543210",
  "password": "$2a$10$...",          // BCrypt hashed
  "provider": "PASSWORD",            // PASSWORD | GOOGLE
  "roles": ["ROLE_USER"],
  "status": "ACTIVE",                // ACTIVE | SUSPENDED | DELETED
  "isTwoFactorEnabled": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "lastLogin": "2024-01-20T14:45:00Z"
}
```

#### RefreshToken Entity
```java
{
  "id": "token_id",
  "userId": "USR_abc123",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiryTime": "2024-02-20T10:30:00Z",
  "revoked": false
}
```

#### ForgotPasswordOtp Entity
```java
{
  "id": "otp_id",
  "email": "user@example.com",
  "otp": "123456",
  "expiryTime": "2024-01-20T10:35:00Z",
  "used": false
}
```

---

## Tracker Service

**Base URL**: `http://localhost:8082`

### Purpose
Manages user portfolios, stock holdings, transactions, market data, and user profiles.

---

### Account Management

#### 1. Create Account
```http
POST /api/accounts
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "userId": "USR_abc123",
  "accountName": "Demat Account 1"
}
```

**Response** (200 OK): No content

**Business Logic**:
- Generates unique accountId
- Creates Account entity
- Initializes empty portfolio for account
- Associates account with userId

---

#### 2. Get User Accounts
```http
GET /api/accounts?userId=USR_abc123
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
[
  {
    "userId": "USR_abc123",
    "accountId": "ACC_xyz789",
    "accountName": "Demat Account 1",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": "USR_abc123",
    "accountId": "ACC_def456",
    "accountName": "Trading Account",
    "createdAt": "2024-01-16T11:00:00Z"
  }
]
```

---

#### 3. Update Account Name
```http
PATCH /api/accounts/{accountId}?userId=USR_abc123&accountName=New%20Name
Authorization: Bearer <access_token>
```

**Response** (200 OK): No content

---

### Portfolio Management

#### 4. Initialize Portfolio
```http
POST /api/portfolio/init?userId=USR_abc123&accountId=ACC_xyz789
Authorization: Bearer <access_token>
```

**Response** (200 OK): No content

**Business Logic**:
- Creates empty UserPortfolio for account
- Sets totalInvestment and totalCurrentValue to 0
- Initializes empty stocks array

---

#### 5. Get Portfolio
```http
GET /api/portfolio?userId=USR_abc123&accountId=ACC_xyz789
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "accountName": "Demat Account 1",
  "stocks": [
    {
      "stockName": "Reliance Industries",
      "isin": "INE002A01018",
      "quantity": 10,
      "averageBuyPrice": 2450.50,
      "currentPrice": 2580.75,
      "currentValue": 25807.50,
      "unrealisedPL": 1302.50,
      "returnPercentage": 5.32,
      "priceUnavailable": false
    }
  ],
  "soldStocks": [
    {
      "stockName": "TCS",
      "isin": "INE467B01029",
      "quantitySold": 5,
      "averageBuyPrice": 3200.00,
      "sellPrice": 3450.00,
      "investedValue": 16000.00,
      "soldValue": 17250.00,
      "realisedPL": 1250.00,
      "soldAt": "2024-01-18T09:30:00Z"
    }
  ],
  "totalInvestment": 24505.00,
  "totalCurrentValue": 25807.50,
  "totalUnrealisedPL": 1302.50,
  "totalRealisedPL": 1250.00,
  "updatedAt": "2024-01-20T14:30:00Z"
}
```

**Business Logic**:
- Fetches portfolio from database
- Retrieves latest market prices for all ISINs
- Falls back to buy price if market price unavailable
- Calculates current value, P/L, and return % for each stock
- **Sorts stocks alphabetically by name**
- Aggregates totals
- Includes sold stocks history

---

#### 6. Confirm Sold Stocks
```http
POST /api/portfolio/confirm-sold
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "soldStocks": [
    {
      "stockName": "Infosys",
      "isin": "INE009A01021",
      "quantity": 8,
      "averageBuyPrice": 1450.00,
      "sellPrice": 1520.00,
      "investedValue": 11600.00
    }
  ]
}
```

**Response** (200 OK): No content

**Business Logic**:
- Calculates soldValue = sellPrice × quantity
- Calculates realisedPL = soldValue - investedValue
- Creates SoldStock entities with soldAt timestamp
- Saves to sold_stocks collection

---

### Transaction Management

#### 7. Buy Stock
```http
POST /api/portfolio/transaction/buy
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "stockName": "HDFC Bank",
  "isin": "INE040A01034",
  "quantity": 15,
  "buyPrice": 1650.00
}
```

**Response** (200 OK): No content

**Business Logic**:
- Finds portfolio by userId and accountId
- Checks if stock already exists in portfolio
- **If exists**: Recalculates average buy price using weighted average
  ```
  newAvgPrice = (oldQty × oldAvgPrice + newQty × newPrice) / (oldQty + newQty)
  ```
- **If new**: Adds new StockHolding
- Updates totalInvestment
- Saves portfolio

---

#### 8. Sell Stock
```http
POST /api/portfolio/transaction/sell
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "stockName": "HDFC Bank",
  "isin": "INE040A01034",
  "quantitySold": 5,
  "sellPrice": 1720.00,
  "soldAt": "2024-01-20T10:00:00Z"
}
```

**Response** (200 OK): No content

**Business Logic**:
- Validates stock exists in portfolio
- Validates sufficient quantity available
- Calculates investedValue = avgBuyPrice × quantitySold
- Calculates soldValue = sellPrice × quantitySold
- Calculates realisedPL = soldValue - investedValue
- Creates SoldStock record
- **If full quantity sold**: Removes stock from portfolio
- **If partial**: Reduces quantity, keeps same avgBuyPrice
- Updates totalInvestment
- Saves both portfolio and sold stock

---

### Excel Upload (Smart Diff)

#### 9. Upload Excel File
```http
POST /api/portfolio/upload
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

userId: USR_abc123
accountId: ACC_xyz789
mode: UPDATE
file: <excel_file>
```

**Excel Format**:
| Stock Name | ISIN | Quantity | Average Buy Price |
|------------|------|----------|-------------------|
| Reliance | INE002A01018 | 10 | 2450.50 |
| TCS | INE467B01029 | 5 | 3200.00 |

**Response** (200 OK):
```json
{
  "status": "NEEDS_CONFIRMATION",
  "totalStocks": 2,
  "addedStocks": 1,
  "updatedStocks": 1,
  "potentiallySoldStocks": 1,
  "detectedSoldStocks": [
    {
      "stockName": "Infosys",
      "isin": "INE009A01021",
      "quantity": 8,
      "averageBuyPrice": 1450.00,
      "investedValue": 11600.00
    }
  ],
  "message": "Portfolio updated. 1 stock(s) appear to be sold. Please confirm."
}
```

**Business Logic - Smart Diff Algorithm**:
1. **Parse Excel**: Extract stock data using Apache POI
2. **Create Maps**: Current portfolio stocks and new Excel stocks (keyed by ISIN)
3. **Detect Changes**:
   - **New stocks**: In Excel but not in portfolio → Add
   - **Updated stocks**: In both but quantity/price changed → Update
   - **Sold stocks**: In portfolio but not in Excel → Flag for confirmation
4. **Update Portfolio**: Replace stocks with Excel data
5. **Return Response**:
   - If sold stocks detected → status = "NEEDS_CONFIRMATION"
   - If no sold stocks → status = "SUCCESS"
6. **User Confirmation Flow** (Frontend):
   - Show detected sold stocks
   - Prompt for sell price for each
   - Call `/api/portfolio/confirm-sold` endpoint

**Key Features**:
- **Idempotent**: Can upload same file multiple times
- **Non-destructive**: Doesn't delete data without confirmation
- **Smart Detection**: Automatically identifies portfolio changes
- **User Control**: Requires explicit confirmation for sold stocks

---

### Market Data

#### 10. Get Stock Chart & Quote
```http
GET /api/yahoo/chart/{isin}?interval=1d&range=1mo
Authorization: Bearer <access_token>
```

**Parameters**:
- `interval`: 1m, 5m, 15m, 1h, 1d, 1wk, 1mo
- `range`: 1d, 5d, 1mo, 3mo, 6mo, 1y, 5y, max

**Response** (200 OK):
```json
{
  "chart": {
    "result": [{
      "meta": {
        "symbol": "RELIANCE.NS",
        "regularMarketPrice": 2580.75,
        "previousClose": 2550.00,
        "currency": "INR"
      },
      "timestamp": [1705737600, 1705824000, ...],
      "indicators": {
        "quote": [{
          "open": [2550.00, 2560.00, ...],
          "high": [2590.00, 2595.00, ...],
          "low": [2545.00, 2555.00, ...],
          "close": [2580.75, 2585.00, ...],
          "volume": [5000000, 4800000, ...]
        }]
      }
    }]
  }
}
```

**Business Logic**:
- Converts ISIN to Yahoo symbol (e.g., INE002A01018 → RELIANCE.NS)
- Fetches data from Yahoo Finance API
- Caches results for performance
- Falls back to NSE/BSE exchanges if primary fails

---

#### 11. Search Tickers
```http
GET /api/ticker/search/{query}
Authorization: Bearer <access_token>
```

**Example**: `/api/ticker/search/reliance`

**Response** (200 OK):
```json
[
  {
    "isin": "INE002A01018",
    "symbol": "RELIANCE",
    "name": "Reliance Industries Limited",
    "exchange": "NSE"
  },
  {
    "isin": "INE002A01018",
    "symbol": "RELIANCE",
    "name": "Reliance Industries Limited",
    "exchange": "BSE"
  }
]
```

---

#### 12. Get Symbol by ISIN
```http
GET /api/ticker/symbol/{isin}
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
"RELIANCE.NS"
```

---

### User Profile

#### 13. Get Profile
```http
GET /api/profile
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "userId": "USR_abc123",
  "email": "user@example.com",
  "name": "John Doe",
  "phoneNumber": "+919876543210",
  "dateOfBirth": "1990-05-15",
  "address": "123 Main St, Mumbai",
  "panNumber": "ABCDE1234F",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Business Logic**:
- Extracts userId and email from JWT token (via JwtAuthFilter)
- Fetches or creates TrackerUser profile
- Returns profile data

---

#### 14. Update Profile
```http
PATCH /api/profile
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "name": "John Doe",
  "phoneNumber": "+919876543210",
  "dateOfBirth": "1990-05-15",
  "address": "123 Main St, Mumbai",
  "panNumber": "ABCDE1234F"
}
```

**Response** (200 OK):
```json
{
  "userId": "USR_abc123",
  "email": "user@example.com",
  "name": "John Doe",
  "phoneNumber": "+919876543210",
  "dateOfBirth": "1990-05-15",
  "address": "123 Main St, Mumbai",
  "panNumber": "ABCDE1234F",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

#### 15. Get User Investment Summary
```http
GET /api/user/summary?userId=USR_abc123
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "userId": "USR_abc123",
  "totalAccounts": 2,
  "totalInvestment": 150000.00,
  "totalCurrentValue": 165000.00,
  "totalUnrealisedPL": 15000.00,
  "totalRealisedPL": 5000.00,
  "totalPL": 20000.00,
  "returnPercentage": 13.33
}
```

**Business Logic**:
- Fetches all accounts for user
- Aggregates portfolio data across all accounts
- Calculates overall return percentage
- Includes both realised and unrealised P/L

---

#### 16. Health Check
```http
GET /health
```

**Response** (200 OK):
```json
"Tracker service is up"
```

---

## Data Models

### Core Entities

#### UserPortfolio
```java
{
  "id": "portfolio_id",
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "accountName": "Demat Account 1",
  "stocks": [
    {
      "stockName": "Reliance Industries",
      "isin": "INE002A01018",
      "quantity": 10,
      "averageBuyPrice": 2450.50,
      "buyValue": 24505.00,
      "lastUpdated": "2024-01-20T10:00:00Z"
    }
  ],
  "totalInvestment": 24505.00,
  "totalCurrentValue": 25807.50,
  "updatedAt": "2024-01-20T14:30:00Z"
}
```

#### SoldStock
```java
{
  "id": "sold_id",
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "stockName": "TCS",
  "isin": "INE467B01029",
  "quantitySold": 5,
  "averageBuyPrice": 3200.00,
  "sellPrice": 3450.00,
  "investedValue": 16000.00,
  "soldValue": 17250.00,
  "realisedPL": 1250.00,
  "soldAt": "2024-01-18T09:30:00Z"
}
```

#### Account
```java
{
  "id": "account_id",
  "userId": "USR_abc123",
  "accountId": "ACC_xyz789",
  "accountName": "Demat Account 1",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### Ticker
```java
{
  "id": "ticker_id",
  "isin": "INE002A01018",
  "symbol": "RELIANCE",
  "name": "Reliance Industries Limited",
  "exchange": "NSE"
}
```

---

## Security

### JWT Authentication Flow

```
1. User Login/Register
   ↓
2. Auth Service generates JWT
   - Access Token (24h expiry)
   - Refresh Token (30d expiry)
   ↓
3. Client stores tokens
   ↓
4. Client sends Access Token in Authorization header
   ↓
5. JwtAuthFilter validates token
   - Extracts userId, email, roles
   - Sets in request attributes
   ↓
6. Controller accesses user info from request
   ↓
7. When Access Token expires
   - Client calls /refresh-token
   - Gets new Access Token
```

### JWT Token Structure

**Access Token Claims**:
```json
{
  "sub": "USR_abc123",
  "roles": ["ROLE_USER"],
  "email": "user@example.com",
  "iat": 1705737600,
  "exp": 1705824000
}
```

**Refresh Token Claims**:
```json
{
  "sub": "USR_abc123",
  "iat": 1705737600,
  "exp": 1708329600
}
```

### Security Configuration

**Auth Service**:
- Public endpoints: `/api/auth/**`, `/health`
- All other endpoints require authentication

**Tracker Service**:
- Public endpoints: `/health`
- All `/api/**` endpoints require valid JWT

### Password Security
- **Hashing**: BCrypt with strength 10
- **Validation**: Minimum 6 characters
- **Storage**: Never stored in plain text

---

## Error Handling

### Global Exception Handler

All exceptions are caught and formatted consistently:

```json
{
  "timestamp": "2024-01-20T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists",
  "path": "/api/auth/register"
}
```

### Custom Exceptions

#### Auth Service
- `UserAlreadyExistsException` (409 Conflict)
- `InvalidRequestException` (400 Bad Request)
- `RefreshTokenExpiredException` (401 Unauthorized)

#### Tracker Service
- `InvalidRequestException` (400 Bad Request)
- `ResourceNotFoundException` (404 Not Found)

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful GET, PATCH, DELETE |
| 201 | Created | Successful POST (resource created) |
| 400 | Bad Request | Validation errors, invalid input |
| 401 | Unauthorized | Invalid/expired JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., email exists) |
| 500 | Internal Server Error | Unexpected server errors |

---

## Best Practices Implemented

### 1. **Separation of Concerns**
- Controllers handle HTTP requests/responses
- Services contain business logic
- Repositories handle data access
- DTOs for data transfer

### 2. **Immutability**
- DTOs use `@Builder` and `@Getter` (no setters)
- Prevents accidental data modification

### 3. **Validation**
- Input validation at service layer
- Meaningful error messages
- Early failure detection

### 4. **Security**
- JWT for stateless authentication
- BCrypt for password hashing
- CORS configuration for frontend
- No sensitive data in logs

### 5. **Performance**
- Market price caching
- Batch price fetching
- Efficient MongoDB queries
- Connection pooling

### 6. **Maintainability**
- Clear naming conventions
- Comprehensive logging
- Modular architecture
- Consistent code style

### 7. **Scalability**
- Stateless services
- Horizontal scaling ready
- Database indexing
- Async operations where applicable

---

## API Usage Examples

### Complete User Flow

```bash
# 1. Register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass123"}'

# Response: {"accessToken":"...", "userId":"USR_abc123"}

# 2. Create Account
curl -X POST http://localhost:8082/api/accounts \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"userId":"USR_abc123","accountName":"My Account"}'

# 3. Buy Stock
curl -X POST http://localhost:8082/api/portfolio/transaction/buy \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId":"USR_abc123",
    "accountId":"ACC_xyz789",
    "stockName":"Reliance",
    "isin":"INE002A01018",
    "quantity":10,
    "buyPrice":2450.50
  }'

# 4. Get Portfolio
curl -X GET "http://localhost:8082/api/portfolio?userId=USR_abc123&accountId=ACC_xyz789" \
  -H "Authorization: Bearer <access_token>"

# 5. Upload Excel
curl -X POST http://localhost:8082/api/portfolio/upload \
  -H "Authorization: Bearer <access_token>" \
  -F "userId=USR_abc123" \
  -F "accountId=ACC_xyz789" \
  -F "mode=UPDATE" \
  -F "file=@holdings.xlsx"
```

---

## Deployment Considerations

### Environment Variables

**Auth Service**:
```properties
MONGO_URI=mongodb://localhost:27017/auth_db
JWT_SECRET=your_secret_key_here
JWT_EXPIRATION=86400000
REFRESH_TOKEN_EXPIRATION=2592000000
GOOGLE_CLIENT_ID=your_google_client_id
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
```

**Tracker Service**:
```properties
MONGO_URI=mongodb://localhost:27017/tracker_db
JWT_SECRET=your_secret_key_here
YAHOO_FINANCE_API_URL=https://query1.finance.yahoo.com
```

### Production Checklist
- [ ] Use strong JWT secrets (256-bit minimum)
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Set up MongoDB authentication
- [ ] Enable rate limiting
- [ ] Configure logging levels
- [ ] Set up monitoring (Prometheus, Grafana)
- [ ] Implement health checks
- [ ] Configure backup strategy
- [ ] Set up CI/CD pipeline

---

## Conclusion

This API documentation provides a comprehensive overview of the Stock Portfolio Tracker system. The architecture follows industry best practices with clear separation of concerns, robust security, and scalable design patterns.

For questions or contributions, please refer to the project repository.

**Version**: 1.0.0  
**Last Updated**: January 2024
