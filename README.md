# NixChat

**Open-source desktop messenger with end-to-end encryption, built in Java.**

NixChat is a real-time messenger developed as a university team project (3rd semester, Fall 2026). The goal is a fast, secure, Telegram-level experience for the core messaging features: the server relays messages without ever being able to read them.

> 🚧 **Status:** in active development. Current milestone: a shared chat room — the JavaFX client and the server exchange messages in real time over WebSocket.

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
├── common/   # Shared DTOs, message protocol, crypto utilities
├── server/   # Spring Boot backend
└── client/   # JavaFX desktop client
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 21+ (if it's missing, Gradle downloads it automatically on the first build)
- Docker and Docker Compose (needed once the database is added)

On Windows, use `gradlew.bat` instead of `./gradlew`.

### Build and test
```bash
./gradlew build
```

### Run the server
```bash
./gradlew :server:bootRun
```
The server starts on `http://localhost:8080`; health check: `http://localhost:8080/actuator/health`.

### Run the client
```bash
./gradlew :client:run
```
Start two clients to chat between them. To connect to another machine:
```bash
./gradlew :client:run --args="--host=192.168.1.10 --port=8080"
```

### Run the infrastructure
```bash
docker compose up -d
```
This starts PostgreSQL, Redis and MinIO for local development.

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
