# Project Structure Overview

```
ALang-backend/
│
├── pom.xml                                    # Maven configuration with all dependencies
│
├── src/main/
│   ├── java/com/alang/
│   │   │
│   │   ├── AlangApplication.java              # Main Spring Boot application
│   │   │
│   │   ├── controller/                        # HTTP Layer (THIN - no business logic)
│   │   │   ├── UserController.java            # POST /user/login, /user/signup, GET /user/me
│   │   │   ├── ChatController.java            # POST /chat/message ⚠️ Does NOT call LLM
│   │   │   ├── NoteController.java            # GET/PATCH/DELETE /notes
│   │   │   ├── ReviewController.java          # GET /review/queue, POST /notes/reviewed
│   │   │   └── MetaController.java            # GET /meta/languages, /meta/starter-prompts
│   │   │
│   │   ├── service/                           # Business Logic Layer
│   │   │   ├── LLMService.java                # ⚠️ ONLY place that calls LLM API
│   │   │   ├── ChatService.java               # Chat orchestration, summarization
│   │   │   ├── NoteService.java               # Note CRUD, de-duplication
│   │   │   ├── ReviewService.java             # Spaced repetition (SM-2 algorithm)
│   │   │   ├── UserService.java               # Authentication, JWT generation
│   │   │   └── impl/                          # Service implementations (stubs)
│   │   │       ├── LLMServiceImpl.java
│   │   │       └── ChatServiceImpl.java
│   │   │
│   │   ├── repository/                        # Data Access Layer
│   │   │   ├── UserRepository.java
│   │   │   ├── LanguageRepository.java
│   │   │   ├── NoteRepository.java
│   │   │   ├── ConversationSummaryRepository.java
│   │   │   ├── RecentMessageRepository.java   # Temporary messages with TTL
│   │   │   └── ReviewEventRepository.java
│   │   │
│   │   ├── entity/                            # JPA Entities (skeleton only)
│   │   │   ├── User.java                      # User accounts, token tracking
│   │   │   ├── Language.java                  # Supported languages (ja, es, fr...)
│   │   │   ├── Note.java                      # Auto-extracted learning notes
│   │   │   ├── ConversationSummary.java       # ⚠️ Condensed chat history
│   │   │   ├── RecentMessage.java             # ⚠️ Temporary (has TTL)
│   │   │   └── ReviewEvent.java               # Review history for analytics
│   │   │
│   │   ├── dto/                               # Request/Response DTOs
│   │   │   ├── auth/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── SignupRequest.java
│   │   │   │   ├── AuthResponse.java
│   │   │   │   └── UserResponse.java
│   │   │   │
│   │   │   ├── chat/
│   │   │   │   ├── ChatMessageRequest.java    # User message + intent/depth
│   │   │   │   ├── ChatMessageResponse.java   # Reply + created notes
│   │   │   │   ├── ChatHistoryDto.java        # Summaries (NOT raw messages)
│   │   │   │   ├── ConversationSummaryDto.java
│   │   │   │   ├── RecentExchangeDto.java
│   │   │   │   └── TokenUsageDto.java         # Token tracking for cost control
│   │   │   │
│   │   │   ├── note/
│   │   │   │   ├── NoteDto.java               # Full note details
│   │   │   │   ├── NotePreviewDto.java        # Minimal preview
│   │   │   │   ├── UpdateNoteRequest.java
│   │   │   │   └── NoteListResponse.java
│   │   │   │
│   │   │   ├── review/
│   │   │   │   ├── ReviewQueueResponse.java   # Notes due for review
│   │   │   │   └── ReviewSubmissionRequest.java
│   │   │   │
│   │   │   └── meta/
│   │   │       ├── LanguageDto.java
│   │   │       └── StarterPromptDto.java
│   │   │
│   │   ├── config/                            # Spring Configuration
│   │   │   ├── SecurityConfig.java            # JWT auth (stub), CORS
│   │   │   └── WebConfig.java                 # Additional MVC config
│   │   │
│   │   └── exception/                         # Custom Exceptions
│   │       ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │       ├── InvalidCredentialsException.java
│   │       ├── NoteNotFoundException.java
│   │       ├── UserNotFoundException.java
│   │       ├── UnauthorizedException.java
│   │       ├── RateLimitExceededException.java
│   │       ├── LLMProviderException.java
│   │       └── EmailAlreadyExistsException.java
│   │
│   └── resources/
│       └── application.yml                    # App configuration, DB, LLM settings
│
├── .gitignore                                 # Git ignore (includes secrets)
├── README.md                                  # Comprehensive architectural overview
├── IMPLEMENTATION_NOTES.md                    # Phase-by-phase implementation guide
└── STRUCTURE.md                               # This file
```

## Key Files to Understand

### 1. LLMService.java (⚠️ CRITICAL)
The ONLY place that calls external LLM APIs. Contains:
- Model selection logic (cheap vs premium)
- Context assembly (summaries + recent messages)
- Token budget enforcement
- Prompt engineering
- Response parsing
- Note extraction

### 2. ConversationSummary.java
The solution to unbounded chat history. Contains detailed explanation of:
- Why summaries instead of raw messages
- When to trigger summarization
- How to use summaries for context

### 3. ChatController.java
Example of THIN controller pattern:
- No LLM calls
- No business logic
- Just HTTP request/response handling

### 4. application.yml
Configuration for:
- Database connection
- JWT settings
- LLM provider settings
- Token limits
- Summarization thresholds

## What's Implemented vs. What's NOT

### ✅ Fully Scaffolded
- All DTOs with example data
- All entities with JPA annotations
- All service interfaces with architectural comments
- All controllers with endpoint definitions
- All repositories with query methods
- Complete package structure
- Configuration files
- Exception handling structure

### ❌ NOT Implemented (See IMPLEMENTATION_NOTES.md)
- Actual LLM API integration
- JWT authentication filter
- Service implementations (only stubs)
- Database migrations
- Tests
- Logging/monitoring

## Critical Architectural Patterns

### 1. Centralized LLM Logic
```
Frontend → ChatController → ChatService → LLMService → External LLM API
                                                      ↑
                                                   ONLY HERE
```

### 2. Conversation Summarization
```
Recent Messages (TTL 48h) → Summarize every 10 messages → ConversationSummary
                          ↓
                    Delete old messages
```

### 3. Auto Note Extraction
```
User Message → LLM Reply (includes structured notes) → Parse → Save to DB → Return to user
```

### 4. Cost Control
```
Every Request:
1. Check user token budget
2. Select model based on intent/depth/tier
3. Track token usage
4. Enforce monthly limits
```

## Next Steps

1. Read [README.md](README.md) for full architectural overview
2. Read [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) for implementation plan
3. Start with Phase 1: Database setup + JWT auth
4. Then Phase 2: LLM integration
