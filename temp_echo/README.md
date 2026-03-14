# ECHO

ECHO is a full-stack application I developed for AI-powered complaint management. It enables users (e.g., leaders or officials) to submit, analyze, and resolve public complaints using Google's Gemini AI, with secure authentication and real-time status updates.

Built with a modern Kotlin/Ktor backend and React/TypeScript frontend. Repository: [nikitabhati147/echo-demo](https://github.com/nikitabhati147).

## Features
- **AI Complaint Analysis**: Gemini-powered endpoint to categorize and summarize complaints.
- **User Authentication**: JWT-based signup, login, and protected routes.
- **Complaint Management**: Create, list, view details, update status, and resolve complaints.
- **Responsive UI**: Modern React app with dashboard, forms, and status chips.
- **Database**: MongoDB with configurable local/Atlas support.
- **Monitoring & Security**: Structured logging, CORS, serialization.

## Installation
1. Clone the repo:
   ```
   git clone https://github.com/nikitabhati147/echo-demo.git
   cd echo-demo
   ```
2. **Backend** (`echo-backend/`):
   - Copy `.env.example` to `.env` and add `GEMINI_API_KEY`, `MONGODB_URI`, `MONGODB_DATABASE`.
   - Ensure Java 25 and MongoDB running.
   - Run: `./gradlew run`
3. **Frontend** (`echo-frontend/`):
   - Run: `npm install`
   - Run: `npm run dev`

Backend defaults to `http://localhost:8080`, frontend to `http://localhost:5173`.

## Usage
- **Seeded Demo Account**: `leader@echo.local` / `leader123` (leader role).
- **Key Endpoints**:
  | Method | Endpoint | Description |
  |--------|----------|-------------|
  | GET | `/` | Root |
  | POST | `/auth/signup`, `/auth/login` | Auth |
  | POST | `/ai/complaints/analyze` | AI analysis |
  | POST/GET/PATCH | `/complaints`*, `/complaints/{id}`*, `/complaints/{id}/status`, `/complaints/{id}/resolve` | CRUD |
- Frontend: Login → Dashboard → Submit/View/Resolve complaints.

## License
This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

