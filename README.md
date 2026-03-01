# Stock Tracker

A microservices-based stock portfolio tracking application with AI-powered features.

## 🏗️ Architecture

```
Client → Nginx → Docker Network → Microservices
```

## 🚀 Services

| Service | Port | Description |
|---------|------|-------------|
| Auth Service | 8081 | User authentication, payments, profile management |
| Tracker Service | 8082 | Portfolio tracking, stock data, news aggregation |
| AI Service | 8084 | AI chat assistant, image generation |
| SheetNews Service | 3005 | Google Sheets integration for news |

## 🌐 Live Application

- **Auth API**: https://auth.ash07.in
- **Tracker API**: https://tracker.ash07.in
- **AI API**: https://ai.ash07.in
- **News API**: https://news.ash07.in

## 🛠️ Tech Stack

**Backend:**
- Spring Boot (Java 21) - Auth & Tracker services
- FastAPI (Python 3.11) - AI service
- Node.js (18) - SheetNews service

**Infrastructure:**
- Docker & Docker Compose
- Nginx (Reverse Proxy)
- MongoDB (Database)

**External Services:**
- Google OAuth, Cloudinary, Razorpay
- Google Gemini AI, OpenRouter
- Yahoo Finance, Fyers, Notion API

## 📦 Project Structure

```
Stock-Tracker/
├── auth-service/          # Authentication & user management
├── tracker-service/       # Portfolio & stock tracking
├── ai-service/           # AI features
├── SheetNews/            # News aggregation
├── mobile-app-v3/        # React Native mobile app (private)
└── docker-compose.yml    # Production deployment config
```

## 🔐 Security

- All secrets excluded from Git
- Services exposed only via Nginx
- Internal Docker network communication
- JWT-based authentication

## 📚 Documentation

For deployment and configuration details, see:
- `DEPLOYMENT-GUIDE.md` - Complete deployment instructions
- `PRODUCTION-SETUP.md` - Architecture and Nginx configuration

## 📄 License

Private repository - All rights reserved

---

**Note:** Configuration files (`.env`, `application.yaml`) are not included in this repository for security reasons. See deployment documentation for setup instructions.
