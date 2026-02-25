# ALang Backend

## What This Project Is

ALang is a self-directed language learning system centered around an LLM-powered chatbot. Users ask questions about vocabulary and grammar, receive explanations, and the system automatically extracts and stores structured notes. Notes can later be reviewed using spaced repetition (Anki-lite style).

This is NOT a lesson-based app. It is a personal language knowledge system.

ALang is language-agnostic: it is designed for speakers of **any** language to learn **any** other language. A Japanese speaker can learn English, a Spanish speaker can learn Korean, etc. Each user has a `nativeLanguageCode` so the LLM delivers explanations in the language they already understand.

## Core Architectural Rules

- **Frontend NEVER talks directly to the LLM.** All LLM interaction is centralized in `LLMService`.
- **No controller should ever call an LLM directly.** Controllers are thin HTTP adapters that delegate to services.
- **Chat history is session-scoped.** Recent messages have a TTL and are deleted after the session ends. Notes serve as the long-term memory — they are injected as context in future LLM calls instead of raw conversation history.
- **Token usage must be controlled.** Model selection (cheap vs premium) is based on intent, depth, and user tier. Token budgets are enforced per-user.
- **Notes are auto-extracted from chat responses**, not manually created by users. Users can edit them afterward.

## Tech Stack

- Java 17, Spring Boot 3.2, Maven
- PostgreSQL with Flyway migrations
- Spring Security with JWT authentication
- External LLM API (provider-agnostic, configured in `application.yml`)
- Lombok for boilerplate reduction

## Project Structure

```
src/main/java/com/alang/
  controller/    Thin HTTP layer — no business logic, no LLM calls
  service/       Business logic interfaces
  service/impl/  Implementations
  entity/        JPA entities
  repository/    Spring Data JPA repositories
  dto/           Request/response objects (auth/, chat/, note/, review/, meta/)
  config/        SecurityConfig, WebConfig
  exception/     Custom exceptions + GlobalExceptionHandler
```

Key service: `LLMService` is the ONLY place that calls external LLM APIs. `ChatService` orchestrates the flow (message → LLM reply → note extraction).

## API Endpoints (v1)

```
POST /user/signup          POST /user/login           GET  /user/me
POST /user/me/languages
POST /chat/message         GET  /chat/history
GET  /notes                GET  /notes/{id}           PATCH /notes/{id}
GET  /review/queue         POST /notes/reviewed
GET  /meta/languages       GET  /meta/starter-prompts
```

## Current Status

Weeks 1–4 complete. Week 5 (Production Readiness) is next.

---

## 6-Week Implementation Plan

### Week 1 — Core Infrastructure

Goal: Database running, auth working end-to-end.

1. **Database setup** ✅
   - [x] Add Flyway dependency
   - [x] Create V1__initial_schema.sql (9 tables + indexes)
   - [x] Create V2__seed_languages.sql (9 languages)
   - [x] Verify migrations run against local PostgreSQL

2. **JWT authentication** ✅
   - [x] Create `JwtTokenProvider` (generate token, validate token, extract user ID)
   - [x] Create `JwtAuthenticationFilter` (reads Authorization header, sets SecurityContext)
   - [x] Create `UserServiceImpl` (signup with BCrypt, login, getCurrentUser)
   - [x] Wire JWT filter into `SecurityConfig` (public: `/user/**`, `/meta/**`; protected: everything else)
   - [x] Wire `UserController` to call `UserService` (remove stub exceptions)

3. **Basic testing** ✅
   - [x] Test JWT generation/validation
   - [x] Test signup → login → access protected endpoint flow

### Week 2 — LLM Integration

Goal: Chat endpoint returns real LLM responses.

1. **LLM service implementation** ✅
   - [x] Add HTTP client (WebClient) for LLM API calls
   - [x] Design prompt templates (system prompt with language tutor role)
   - [x] Implement `LLMServiceImpl.generateReply()`
   - [x] Implement `LLMServiceImpl.selectModel()` (cheap vs standard based on user tier)
   - [x] Add error handling (rate limits, timeouts, retries with backoff)

2. **Chat service implementation** ✅
   - [x] Complete `ChatServiceImpl.sendMessage()` (save message → call LLM → save reply → return response)
   - [x] Implement message saving to `RecentMessage` table
   - [x] Wire up `ChatController` to call `ChatService`

3. **Token tracking** ✅
   - [x] Implement `countTokens()` (approximate or tiktoken)
   - [x] Implement `recordTokenUsage()` (increment user's monthly total)
   - [x] Implement `checkTokenBudget()` (enforce free/premium tier limits)

### Week 3 — Note Extraction

Goal: Generate a note froma chat session/conversation.

1. **Prompt engineering for notes** ✅
   - [x] Design system prompt that instructs LLM to return a proper response for the user's language-learning related questions
   - [x] Design system prompt that instructs LLM to return a structured JSON note based on conversation session
   - [x] Test structured output format across languages
   - [x] Iterate on prompt for quality and consistency

2. **Note extraction logic** ✅
   - [x] Implement `LLMServiceImpl.extractNotes()` (parse JSON from LLM response)
   - [x] Validate extracted note data (type, title, confidence)
   - [x] Handle updating the note of the current

3. **Note service** ✅
   - [x] Create `NoteServiceImpl` (CRUD operations)
   - [x] Implement `findSimilarNotes()` for de-duplication (fuzzy title match)
   - [x] Wire note creation into chat flow (ChatService calls NoteService after extraction)
   - [x] Wire up `NoteController` endpoints

### ~~Week 4 — Conversation Summarization~~ (Skipped)

Notes extracted per session serve as long-term LLM context. Raw messages expire via TTL. No summarization infrastructure needed.

### Week 4 — Review System ✅

Goal: Spaced repetition review working end-to-end.

1. **Review service** ✅
   - [x] Create `ReviewServiceImpl`
   - [x] Implement SM-2 algorithm (`calculateNextInterval`, `updateEaseFactor`)
   - [x] Unit test interval calculations across all quality ratings (1-4)

2. **Review endpoints** ✅
   - [x] Implement `getReviewQueue()` (notes where `nextReviewAt <= now`, ordered by priority)
   - [x] Implement `submitReview()` (update note's interval/ease/nextReview, save ReviewEvent)
   - [x] Wire up `ReviewController`

3. **Analytics** ✅
   - [x] Implement `getReviewStats()` (total notes, reviewed today, due today, streak, retention rate)
   - [x] Wire POST /review/reviewed endpoint in `ReviewController`

### Week 5 — Production Readiness

Goal: Secure, observable, documented, deployable.

1. **Security hardening**
   - [ ] Audit all endpoints for proper authorization checks
   - [ ] Validate all user inputs (request body sizes, string lengths)
   - [ ] Implement rate limiting (per-user request throttling)
   - [ ] Add refresh tokens (`refresh_tokens` table, `POST /auth/refresh` endpoint, rotate on use)
   - [ ] Move all secrets to environment variables
   - [ ] Enforce HTTPS

2. **Error handling and observability**
   - [ ] Add structured logging for all LLM requests/responses
   - [ ] Add monitoring metrics (token usage, latency, error rates)
   - [ ] Test all error scenarios (LLM down, DB down, rate limit hit)
   - [ ] Consider Sentry or similar for error tracking

3. **Performance**
   - [ ] Review database indexes against actual query patterns
   - [ ] Add caching (Caffeine or Redis) if needed
   - [ ] Load test key endpoints

4. **Documentation**
   - [ ] Add OpenAPI/Swagger annotations
   - [ ] Write deployment guide
   - [ ] Write environment setup guide for new developers
