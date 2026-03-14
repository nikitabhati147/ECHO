# echo-backend

Ktor backend for the ECHO prototype.

## Current setup

- Kotlin + Ktor
- Java 25 toolchain
- MongoDB connectivity
- JWT authentication plugin configured
- Gemini complaint-analysis endpoint
- `.env` support for `GEMINI_API_KEY`, `MONGODB_URI`, and `MONGODB_DATABASE`

## Environment

Create a local `.env` file in this directory:

```bash
cp .env.example .env
```

Then place your Gemini key and Mongo config in `.env`:

```bash
GEMINI_API_KEY=your-gemini-api-key
MONGODB_URI=mongodb://127.0.0.1:27017
MONGODB_DATABASE=echo
```

For MongoDB Atlas, use your cluster connection string instead:

```bash
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster-host>/?retryWrites=true&w=majority
MONGODB_DATABASE=echo
```

If `MONGODB_URI` is missing, the backend falls back to the local values in `application.yaml`.

## Useful routes

- `GET /`
- `GET /health`
- `POST /auth/signup`
- `POST /auth/login`
- `GET /auth/me`
- `POST /ai/complaints/analyze`
- `POST /complaints`
- `GET /complaints`
- `GET /complaints/{id}`
- `PATCH /complaints/{id}/status`
- `PATCH /complaints/{id}/resolve`

## Seeded leader account

The backend seeds a demo leader on startup from `application.yaml`:

- `leader@echo.local`
- `leader123`

## Run

```bash
./gradlew run
```

Make sure your IDE/Gradle runtime is using Java 25 for this project.

## Test

```bash
./gradlew test
```

