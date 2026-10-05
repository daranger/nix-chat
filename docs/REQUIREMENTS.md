# Course requirements → Nchat code

Where each project step (Dr. Shirin Noekhan) is implemented.
All core concepts live in plain Java in the `common` and `client` modules, without framework magic.

---

## Step 1 — Conditions, loops, methods, arrays, classes

| Concept | Where |
|---|---|
| `if` / `else if` / `else` | `Contact.lastSeenText()`, `User.isValidUsername()` |
| Ternary operator | `User` constructor, `PrivateChat.idFor()`, `FileMessage.humanSize()` |
| `switch` expression | `FileMessageStorage.decodeLine()` — picks the message type |
| `for-each` loop | `User.isValidUsername()`, `FileMessage.isImage()`, `InMemoryMessageStorage.findByChat()` |
| Indexed `for` loop | `ContactStore.load()` — skips the CSV header |
| `while` loop | `FileMessage.humanSize()`, `FileMessageStorage.load()` (reading lines) |
| Arrays | `FileMessage.SIZE_UNITS`, `FileMessage.IMAGE_EXTENSIONS`, `byte[]` IV/ciphertext in `AesGcmEncryptor`, `String[] parts` when parsing files |
| Methods & classes | 25+ classes across `common`, `client`, `server` |

## Step 2 — OOP

**Inheritance (2 hierarchies)**
1. `Message` → `TextMessage`, `FileMessage`, `SystemMessage`, `TrackMessage` (`common/.../model/message`)
2. `Chat` → `PrivateChat`, `GroupChat` (`common/.../model/chat`)
3. `Department` → six departments (`common/.../department`)
4. `TurnBasedGame` → `TicTacToe` (`common/.../games`)

Bonus: `ChatView extends BorderPane`, `MessageCell` / `ContactCell extends ListCell` (JavaFX).

**Abstract classes with implemented methods (2)**
1. `Message` — abstract `preview()`, `getType()`; implemented `format()`, `formattedTime()`, `isFrom()`
2. `Chat` — abstract `canWrite()`, `getTitle()`, `getMaxParticipants()`; implemented `addParticipant()`, `removeParticipant()`, `isParticipant()`

**Interfaces with implementations (2)**
1. `Encryptor` → `AesGcmEncryptor`, `NoOpEncryptor` (plus default methods `encryptText()` / `decryptText()`)
2. `MessageStorage` → `InMemoryMessageStorage`, `FileMessageStorage` (plus default method `findLast()`)
3. `VerificationSender` (server) → `TelegramGatewaySender`, `DevConsoleSender`; `PhoneConfig` picks one at startup

**Method overriding (3+)**
1. `preview()` — overridden in `TextMessage`, `FileMessage`, `SystemMessage`
2. `canWrite()` / `getTitle()` — overridden in `PrivateChat`, `GroupChat`
3. `format()` — overridden in `TextMessage` (full text) and `SystemMessage` (centered notice)

Also: `User.equals()` / `hashCode()` / `toString()`, `MessageCell.updateItem()`, `ContactCell.updateItem()`.

**Polymorphism (3+)**
1. The chat feed is a `ListView<Message>`; `MessageCell` calls `message.format()` and each type renders itself.
2. `AppSettings.createCacheEncryptor()` returns an `Encryptor` — AES-256-GCM or none, depending on the settings; `FileMessageStorage` doesn't know which.
3. `NchatApp.history` is declared as `MessageStorage` and holds a `FileMessageStorage`.
4. `Chat.canWrite()` — tested on an array `Chat[]` of private and group chats (`ChatTest.canWriteIsPolymorphic`).

## Step 3 — Files and exceptions

**Three files, one per entity** — stored in `~/.nchat/<profile>/`:

| File | Entity | Class |
|---|---|---|
| `settings.properties` | current `User` + connection & encryption settings | `AppSettings` |
| `contacts.csv` | `Contact` | `ContactStore` |
| `messages-cache.tsv` | `Message` (contents encrypted with AES-256-GCM) | `FileMessageStorage` |

**Four different try-catch types**
1. `IOException` — `AppSettings.load()`: settings file unreadable
2. `NumberFormatException` — `AppSettings.getServerPort()`: port edited by hand to non-number
3. `URISyntaxException` — `NchatApp.connect()`: invalid server address
4. `GeneralSecurityException` — `FileMessageStorage.decodeLine()`: wrong key or tampered cache

**Custom exceptions** — `InsufficientFundsException` (checked) in the Wallet department: `WalletService.transfer()` throws it when the balance is too low. `AuthException` (checked) in the client: `ApiClient` throws it when the server refuses a login or registration. On the server, `UsernameTakenException` and `InvalidCredentialsException` become HTTP 409 and 401.

**One try-catch with multiple catches and finally** — `FileMessageStorage.load()`:
`catch (NoSuchFileException)` → `catch (IOException)` → `catch (RuntimeException)` → `finally` (prints load statistics).
Multi-catch syntax is also used: `catch (DateTimeParseException | IllegalArgumentException e)` in `ContactStore.load()`.

## Step 4 — GUI

JavaFX 21 + AtlantaFX dark theme (as in PMC). Every team member must be able to explain the client code:
- `NchatApp` — entry point, wiring
- `ui.ChatView` — layout: contacts sidebar, feed, input bar
- `ui.MessageCell`, `ui.ContactCell` — custom list cells

Screen sketches for QA: [Nchat-Screen-Sketches.pdf](Nchat-Screen-Sketches.pdf).

## Course materials

- [Nchat-Project-Description.docx](Nchat-Project-Description.docx) — project description
- [Nchat-Presentation.pdf](Nchat-Presentation.pdf) — presentation for the defense
- [Nchat-Screen-Sketches.pdf](Nchat-Screen-Sketches.pdf) — screen sketches (Step 4)

## Step 5 — Packages

```
uz.nchat.common                 Protocol
uz.nchat.common.model           User, Role
uz.nchat.common.model.message   Message, TextMessage, FileMessage, SystemMessage   ← subpackage
uz.nchat.common.model.chat      Chat, PrivateChat, GroupChat                      ← subpackage
uz.nchat.common.crypto          Encryptor, AesGcmEncryptor, NoOpEncryptor
uz.nchat.common.storage         MessageStorage, InMemoryMessageStorage
uz.nchat.client                 NchatApp (main package of the client)
uz.nchat.client.net             ChatConnection, WireFormat, ApiClient, AuthException                        ← subpackage
uz.nchat.client.storage         AppSettings, Contact, ContactStore, FileMessageStorage
uz.nchat.client.ui              LoginView, ChatView, MessageCell, ContactCell
uz.nchat.server                 NchatServerApplication
uz.nchat.server.ws              WebSocketConfig, ChatWebSocketHandler, JwtHandshakeInterceptor  ← subpackage
uz.nchat.server.auth            UserAccount, AuthService, TokenService, AuthController
uz.nchat.server.config          SecurityConfig, JwtProperties
```

**Cross-package method calls** (from `uz.nchat.client.NchatApp`):
- `AppSettings.load()` — package `client.storage`
- `WireFormat.decode()` / `encode()` — package `client.net`
- `Protocol.wsUrl()` — package `common`
- `ChatView.showMessage()` — package `client.ui`

## Departments

Like the departments of PMC, Nchat is organised into six departments. Each is a subclass of the abstract
`Department` with four positions (24 in total) and its own code package; `NchatCompany` lists them all.

| Department | Mission | Feature in code | Package |
|---|---|---|---|
| Messaging | Private and group chats | chats, messages, roles | `common.model` |
| Wallet | NCoin virtual currency | balances, transfers, history | `common.wallet` |
| Music | Sharing tracks, playlists | `Track`, `Playlist`, `TrackMessage` | `common.music` |
| Games | Games inside chats | `Game`, `TurnBasedGame`, `TicTacToe` | `common.games` |
| Store | NStore mini-app catalog | publish, search, install, rate | `common.store` |
| Security | Encryption and safety | AES-256-GCM encryption | `common.crypto` |

NCoin is a virtual currency with no real-money value.

## Tests

`./gradlew test` — 32 unit tests: message types, chats and roles, encryption, local files, departments, wallet, music, games, store; plus server tests for registration, login and tokens.
