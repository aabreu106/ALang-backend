# ALang Backend

Backend for a language-learning web application centered around an LLM-powered chatbot.

## Project Status

**This is a SCAFFOLD, not a working application.**

This codebase provides:
- Complete package structure
- All DTOs, entities, services, controllers, repositories
- Comprehensive architectural comments
- Design patterns and best practices

**What's NOT implemented yet:**
- Actual LLM API integration
- JWT authentication
- Database migrations
- Full service implementations
- Tests

## Core Architecture

### 🎯 Design Principles

1. **Frontend NEVER talks directly to LLM**
   - All LLM interaction centralized in `LLMService`
   - Controllers have no LLM logic

2. **Thin Controllers, Fat Services**
   - Controllers only handle HTTP request/response
   - Services contain all business logic

3. **Cost Control First**
   - Token usage tracking
   - Model selection (cheap vs premium)
   - Rate limiting hooks

4. **Conversation Summarization (Not Raw Storage)**
   - Chat history is SUMMARIZED, not replayed
   - Recent messages have TTL
   - Prevents token explosion

### 📦 Package Structure

```
com.alang/
├── controller/        # HTTP layer (THIN)
│   ├── AuthController
│   ├── ChatController      ⚠️ Does NOT call LLM
│   ├── NoteController
│   ├── ReviewController
│   └── MetaController
│
├── service/          # Business logic layer
│   ├── LLMService          ⚠️ ONLY place that calls LLM API
│   ├── ChatService         # Orchestrates chat flow
│   ├── NoteService         # Manages auto-extracted notes
│   ├── ReviewService       # Spaced repetition (Anki-style)
│   └── AuthService
│
├── repository/       # Data access layer
│   ├── UserRepository
│   ├── NoteRepository
│   ├── ConversationSummaryRepository
│   └── RecentMessageRepository
│
├── entity/           # JPA entities (skeleton only)
│   ├── User
│   ├── Note               # Auto-extracted from chat
│   ├── ConversationSummary  # Condensed chat history
│   └── RecentMessage      # Temporary (has TTL)
│
├── dto/              # Request/response DTOs
│   ├── auth/
│   ├── chat/
│   ├── note/
│   └── review/
│
├── config/           # Spring configuration
│   ├── SecurityConfig     # JWT auth (stub)
│   └── WebConfig
│
└── exception/        # Custom exceptions
```

## 🔑 Key Architectural Patterns

### 1. Centralized LLM Service

**Why it matters:**

❌ **BAD** (Controller calling LLM directly):
```java
@PostMapping("/chat")
public Response chat(@RequestBody Request req) {
    String prompt = buildPrompt(req);  // ⚠️ Prompt logic in controller
    String reply = openAI.chat(prompt); // ⚠️ Direct LLM call
    return new Response(reply);
}
```

✅ **GOOD** (LLM logic centralized):
```java
@PostMapping("/chat")
public Response chat(@RequestBody Request req) {
    return chatService.sendMessage(req, userId);
}

// In ChatService:
public Response sendMessage(Request req, String userId) {
    return llmService.generateReply(req, userId); // ← ONLY here
}
```

**Benefits:**
- Cost control in one place
- Easy to switch LLM providers
- Token tracking centralized
- Rate limiting hooks
- Testable without mocking HTTP

### 2. Conversation Summarization

**The Problem:**
- Can't replay 1000 messages into LLM context (token limits)
- Storing all messages forever = database bloat + high costs

**The Solution:**
1. Keep recent messages (last 5-10 exchanges) in `RecentMessage` table
2. After threshold (e.g., 10 messages), call `LLMService.generateSummary()`
3. Store condensed summary in `ConversationSummary` table
4. DELETE old messages after summarization
5. When user sends new message, include summaries (not old messages) in context

**Example:**
```
Messages 1-10:  "User asked about は vs が. Understood topic/subject..."
Messages 11-20: "User learned て-form conjugation..."
Message 21:     "Can you remind me about は?"

Context sent to LLM:
[Summary of 1-10] + [Summary of 11-20] + [Message 21]

NOT:
[Message 1] + [Message 2] + ... + [Message 20] + [Message 21]
```

See `ConversationSummary.java` for detailed comments.

### 3. Auto-Extracted Notes

Notes are NOT manually created by users.
They are AUTOMATICALLY extracted from LLM responses.

**Flow:**
1. User asks: "What's the difference between は and が?"
2. LLM responds with explanation
3. `LLMService.extractNotes()` parses response
4. `NoteService` saves structured note
5. Note is returned to frontend in `ChatMessageResponse`
6. User can edit note later (marks as `userEdited=true`)

**Confidence Scores:**
- Notes have confidence (0.0-1.0)
- Low confidence (<0.3) = needs user review
- High confidence (>0.7) = probably accurate

### 4. Spaced Repetition Review

Anki-style review system with SM-2 algorithm.

**Flow:**
1. User opens app, calls `GET /review/queue`
2. Backend returns notes where `nextReviewAt <= now`
3. User reviews note, rates quality (1-5)
4. Backend calculates next interval using SM-2
5. Updates `nextReviewAt`, `easeFactor`, `intervalDays`

See `ReviewService.java` for algorithm details.

## 📡 API Endpoints

### Auth
```
POST /auth/signup       # Register new user
POST /auth/login        # Get JWT token
GET  /auth/me           # Get current user profile
```

### Chat (Core Feature)
```
POST /chat/message      # Send message, get AI reply + auto-extracted notes
GET  /chat/history      # Get summaries (NOT raw messages)
```

**Example Request:**
```json
POST /chat/message
{
  "language": "ja",
  "message": "What's the difference between は and が?",
  "intent": "grammar_explanation",
  "depth": "normal"
}
```

**Example Response:**
```json
{
  "reply": "は marks the topic, while が marks the subject...",
  "createdNotes": [
    {
      "id": "note_123",
      "type": "grammar",
      "title": "は vs が",
      "confidence": 0.9
    }
  ],
  "tokenUsage": {
    "promptTokens": 150,
    "completionTokens": 200,
    "totalTokens": 350
  },
  "modelUsed": "gpt-4"
}
```

### Notes
```
GET    /notes                # List all notes (with filters)
GET    /notes/{id}           # Get single note
PATCH  /notes/{id}           # Edit note
DELETE /notes/{id}           # Delete note
```

### Review
```
GET  /review/queue           # Get notes due for review
POST /notes/reviewed         # Submit review result (quality 1-5)
```

### Meta
```
GET /meta/languages          # List supported languages
GET /meta/starter-prompts    # Get example questions
```

## 🗄️ Data Model

### Core Entities

**User**
- Basic auth (email, passwordHash)
- Token usage tracking (for cost control)
- Tier (free, premium)

**Language**
- Supported languages (ja, es, fr, etc.)
- Seeded at startup

**Note** (Auto-extracted from chat)
- Type: vocab, grammar, exception
- Title, summary, examples
- Confidence score (from LLM)
- Spaced repetition metadata

**ConversationSummary** ⚠️ Critical for cost control
- Condensed summary of N messages
- Topics covered
- Replaces raw message storage

**RecentMessage** ⚠️ Temporary with TTL
- Last 5-10 messages only
- Deleted after summarization
- Has `expiresAt` timestamp

**ReviewEvent**
- Review history (for analytics)
- Quality ratings (1-5)
- Used for SM-2 algorithm tuning

## 🚀 Getting Started

### Prerequisites
- Java 17+
- PostgreSQL
- Maven

### Setup

1. **Create PostgreSQL database:**
```bash
createdb alang
```

2. **Configure environment variables:**
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret_key
export LLM_API_KEY=your_llm_api_key
```

3. **Update application.yml** (if needed)

4. **Run the application:**
```bash
mvn spring-boot:run
```

### Next Steps (Implementation TODOs)

#### High Priority
1. **Implement JWT Authentication**
   - Create `JwtAuthenticationFilter`
   - Create `JwtTokenProvider`
   - Wire up in `SecurityConfig`

2. **Implement LLM Integration**
   - Add HTTP client in `LLMServiceImpl`
   - Add prompt templates
   - Add response parsing
   - Add error handling

3. **Implement ChatService**
   - Wire up LLMService
   - Implement message saving
   - Implement note extraction flow
   - Implement summarization trigger

4. **Database Migrations**
   - Add Flyway or Liquibase
   - Create initial schema migration
   - Seed languages table

#### Medium Priority
5. **Implement ReviewService**
   - SM-2 algorithm
   - Queue calculation
   - Analytics queries

6. **Add Tests**
   - Unit tests for services
   - Integration tests for controllers
   - Mock LLM responses

7. **Rate Limiting**
   - Token budget enforcement
   - Per-user rate limits
   - Monthly quota tracking

#### Low Priority
8. **Advanced Features**
   - Email verification
   - Password reset
   - Note search (full-text)
   - Note relationships
   - Export functionality

## ⚠️ Critical Design Decisions

### Why Summaries Instead of Raw Messages?

**Problem:** Storing all chat messages forever leads to:
1. **Token explosion:** Can't send 1000 messages to LLM (context limits)
2. **High costs:** Every message in context costs tokens
3. **Database bloat:** Millions of messages
4. **Privacy concerns:** Old messages linger forever

**Solution:** Conversation summarization
- Keep recent messages (5-10) for immediate context
- Summarize older messages into 1-2 paragraphs
- Delete original messages after summarization
- Use summaries for context (10-20x more efficient)

### Why Centralize LLM Logic?

**Problem:** If controllers call LLM directly:
1. Can't control costs (no central token tracking)
2. Can't enforce rate limits
3. Hard to switch providers
4. Prompt engineering scattered everywhere
5. Can't mock for testing

**Solution:** Single `LLMService`
- ALL LLM calls go through here
- Central cost control
- Central prompt engineering
- Easy to mock
- Provider-agnostic

### Why Auto-Extract Notes?

**Problem:** Users won't manually create notes (too much friction).

**Solution:** Auto-extract from LLM responses
- LLM already "knows" what's important
- Include structured notes in LLM response format
- Parse and save automatically
- User can edit later if needed

## 📝 Code Style

- **DTOs:** Immutable where possible, use Lombok `@Data`
- **Services:** Interface + implementation separation
- **Controllers:** Thin, no business logic
- **Comments:** Explain WHY, not WHAT
- **TODOs:** Mark all stubs clearly

## 🧪 Testing Strategy (Not Implemented Yet)

### Unit Tests
- Services (mock repositories)
- LLMService (mock HTTP client)
- ReviewService (test SM-2 algorithm)

### Integration Tests
- Controllers (mock services)
- Repositories (use H2 in-memory DB)

### E2E Tests
- Full chat flow
- Review flow
- Auth flow

## 📊 Monitoring & Observability (Not Implemented Yet)

### Metrics to Track
- Token usage per user
- LLM API latency
- Error rates
- Cost per request
- Review completion rates

### Logging
- All LLM requests/responses
- All errors
- Token usage events
- Summarization events

## 🔒 Security Considerations

### Current State (Stub)
- ⚠️ JWT authentication NOT implemented
- ⚠️ All endpoints currently public
- ⚠️ Passwords NOT hashed (AuthService stub)

### TODO Before Production
1. Implement JWT filter
2. Hash passwords with BCrypt
3. Add CSRF protection
4. Rate limiting per user
5. Input validation (SQL injection, XSS)
6. Secure secrets (use Vault or AWS Secrets Manager)
7. HTTPS only
8. Audit logging

## 📚 References

- [SM-2 Spaced Repetition Algorithm](https://www.supermemo.com/en/archives1990-2015/english/ol/sm2)
- [Spring Security JWT Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [OpenAI API Docs](https://platform.openai.com/docs)

## License

MIT (or your chosen license)
