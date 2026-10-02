# NixChat

**Open-source desktop messenger with end-to-end encryption, built in Java.**

NixChat is a real-time messenger developed as a university team project (3rd semester, Fall 2026). The goal is a fast, secure, Telegram-level experience for the core messaging features: the server relays messages without ever being able to read them.

> 🚧 **Status:** in active development. Current milestone: accounts and login (PostgreSQL, BCrypt, JWT) and a shared chat room in real time over WebSocket; the client keeps an encrypted local history and a contact list.

> 📋 How the code covers the course requirements (Steps 1–5): [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)

---

## ✨ Features

### Core
- Registration and login (JWT-based authentication)
- Private and group chats
- Real-time message delivery over WebSocket
- Message history with pagination
- Online / offline status and "typing…" indicator
- Delivery and read receipts

### Messaging
- File and image sharing
- Reply, forward, edit and delete messages
- Message search
- Offline delivery: messages are queued until the recipient comes online

### Security
- **End-to-end encryption**: X25519 key exchange + AES-GCM
- The server stores and relays only ciphertext
- Passwords hashed with BCrypt

### Departments
- **Wallet** — NixCoin virtual currency: balances and transfers between friends
- **Music** — share tracks in chats, collaborative playlists
- **Games** — mini-games inside chats, starting with tic-tac-toe
- **Store** — NixStore, a catalog of mini-apps: install, search, rate

See [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md#departments) for all six departments and their positions.

### Planned
- Multi-device sync
- Horizontal scaling (multiple server instances via Redis pub/sub)
- Voice messages
- Voice/video calls (WebRTC)

---

## 🛠 Tech Stack

| Layer          | Technology                                         |
|----------------|----------------------------------------------------|
| Language       | Java 21                                            |
| Server         | Spring Boot 4, Spring WebSocket, Spring Security   |
| Client         | JavaFX 21, AtlantaFX                               |
| Database       | PostgreSQL, Flyway migrations                      |
| Cache / PubSub | Redis                                              |
| File storage   | MinIO (S3-compatible)                              |
| Build          | Gradle (Kotlin DSL), multi-module                  |
| Testing        | JUnit 5, Testcontainers, Gatling (load testing)    |
| DevOps         | Docker, Docker Compose, GitHub Actions             |

---

## 📁 Project Structure

```
nix-chat/
├── common/   # Domain model, departments (wallet, music, games, store), encryption
├── server/   # Spring Boot backend
├── client/   # JavaFX desktop client: ui / net / storage packages
└── docs/     # Course requirements mapping
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 21+ (if it's missing, Gradle downloads it automatically on the first build)
- Docker and Docker Compose (needed once the database is added)

On Windows (PowerShell), use `.\gradlew.bat` instead of `./gradlew`.

### 1. Start the database
```bash
docker compose up -d
```
This starts PostgreSQL (plus Redis for later milestones). The server creates its tables on first start.

### 2. Build and test
```bash
./gradlew build
```
Tests use an in-memory H2 database, so they do not need Docker.

### 3. Run the server
```bash
./gradlew :server:bootRun
```
The server starts on `http://localhost:8080`; health check: `http://localhost:8080/actuator/health`.

### 4. Run the client
```bash
./gradlew :client:run
```
Create an account on the first screen, or sign in. The login is remembered until you press **Sign out**.

Start two clients with different profiles to chat between them on one computer:
```bash
./gradlew :client:run --args="--profile=alice"
./gradlew :client:run --args="--profile=bob"
```
Each profile keeps its own files in `~/.nixchat/<profile>/`: `settings.properties`, `contacts.csv` and an encrypted `messages-cache.tsv`.

To connect to another machine:
```bash
./gradlew :client:run --args="--host=192.168.1.10 --port=8080"
```

### API
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | — | `{username, displayName, password}` → `{token, username, displayName}` |
| POST | `/api/auth/login` | — | `{username, password}` → `{token, username, displayName}` |
| GET | `/api/me` | Bearer token | Current user |
| WS | `/ws` | Bearer token | Real-time chat |

---

## 👥 Team

| Name | Area |
|------|------|
| Roman Morgunov | — |
| — | Server: auth, users, chats, REST API |
| — | Server: real-time delivery, WebSocket, Redis |
| — | Client: JavaFX UI |
| — | Encryption, infrastructure, CI, testing |

---

## 🤝 Contributing

1. Create a branch from `main`: `feature/<short-name>`
2. Commit with clear messages
3. Open a Pull Request; at least one teammate reviews before merge

All contributions are made under the project license.

---

## 📄 License

Licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.
