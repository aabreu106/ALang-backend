# Implementation Notes

This document tracks what needs to be implemented and in what order.

## 🎯 Implementation Priority

### Phase 1: Core Infrastructure (Week 1)

**Goal:** Get basic app running with auth

1. **Database Setup**
   - [ ] Add Flyway dependency to pom.xml
   - [ ] Create initial migration (V1__initial_schema.sql)
   - [ ] Seed languages table
   - [ ] Test database connection

2. **JWT Authentication**
   - [ ] Implement `JwtTokenProvider` (generate/validate tokens)
   - [ ] Implement `JwtAuthenticationFilter`
   - [ ] Wire up in `SecurityConfig`
   - [ ] Complete `UserServiceImpl`
   - [ ] Test login/signup flow

3. **Basic Testing**
   - [ ] Add test dependencies
   - [ ] Test auth endpoints
   - [ ] Test JWT token generation/validation

### Phase 2: LLM Integration (Week 2)

**Goal:** Get chat working with LLM

1. **LLM Service Implementation**
   - [ ] Choose LLM provider (OpenAI, Anthropic, etc.)
   - [ ] Add HTTP client (WebClient)
   - [ ] Implement prompt templates
   - [ ] Implement `generateReply()`
   - [ ] Add error handling (rate limits, timeouts)
   - [ ] Add retry logic with exponential backoff

2. **Chat Service Implementation**
   - [ ] Complete `ChatServiceImpl`
   - [ ] Implement message saving to `RecentMessage`
   - [ ] Wire up `LLMService`
   - [ ] Test basic chat flow

3. **Token Tracking**
   - [ ] Implement `countTokens()` (use tiktoken library)
   - [ ] Implement `recordTokenUsage()`
   - [ ] Implement `checkTokenBudget()`
   - [ ] Add monthly limit enforcement

### Phase 3: Note Extraction (Week 3)

**Goal:** Auto-extract notes from chat

1. **Prompt Engineering**
   - [ ] Design system prompt for note extraction
   - [ ] Test LLM's structured output format
   - [ ] Iterate on prompt to improve note quality

2. **Note Extraction**
   - [ ] Implement `LLMService.extractNotes()`
   - [ ] Parse JSON/markdown from LLM response
   - [ ] Validate extracted note data

3. **Note Service**
   - [ ] Complete `NoteServiceImpl`
   - [ ] Implement note de-duplication (find similar notes)
   - [ ] Wire up note creation in chat flow
   - [ ] Test note CRUD operations

### Phase 4: Conversation Summarization (Week 4)

**Goal:** Implement cost-saving summarization

1. **Summarization Logic**
   - [ ] Implement `LLMService.generateSummary()`
   - [ ] Design summarization prompt
   - [ ] Extract topics from summary

2. **Summarization Triggers**
   - [ ] Implement `shouldTriggerSummarization()`
   - [ ] Add message count threshold check
   - [ ] Add token count threshold check

3. **Cleanup Jobs**
   - [ ] Implement scheduled task for expired message cleanup
   - [ ] Implement post-summarization message deletion
   - [ ] Test TTL mechanism

4. **Context Assembly**
   - [ ] Load summaries for context
   - [ ] Load recent messages
   - [ ] Calculate total token budget
   - [ ] Trim context if exceeds budget

### Phase 5: Review System (Week 5)

**Goal:** Implement spaced repetition

1. **Review Service**
   - [ ] Complete `ReviewServiceImpl`
   - [ ] Implement SM-2 algorithm
   - [ ] Test interval calculation
   - [ ] Test ease factor updates

2. **Review Endpoints**
   - [ ] Implement queue retrieval
   - [ ] Implement review submission
   - [ ] Calculate next review dates

3. **Analytics**
   - [ ] Implement review stats calculation
   - [ ] Implement streak tracking

### Phase 6: Production Readiness (Week 6)

**Goal:** Polish for deployment

1. **Security Hardening**
   - [ ] Review all endpoints for authorization
   - [ ] Add input validation
   - [ ] Add rate limiting
   - [ ] Secure secrets (environment variables)
   - [ ] Enable HTTPS

2. **Error Handling**
   - [ ] Test all error scenarios
   - [ ] Add proper logging
   - [ ] Add monitoring/alerting

3. **Performance**
   - [ ] Add database indexes
   - [ ] Add caching (Redis) if needed
   - [ ] Test under load

4. **Documentation**
   - [ ] API documentation (Swagger/OpenAPI)
   - [ ] Deployment guide
   - [ ] Environment setup guide

## 🔧 Technical Decisions to Make

### LLM Provider
**Options:**
- OpenAI (GPT-4, GPT-3.5)
- Anthropic (Claude 3)
- OpenRouter (multiple providers)

**Recommendation:** Start with OpenAI (easiest), design for multi-provider

### Database Migrations
**Options:**
- Flyway (SQL-based)
- Liquibase (XML/YAML-based)

**Recommendation:** Flyway (simpler for SQL developers)

### Token Counting
**Options:**
- tiktoken (OpenAI's tokenizer)
- Approximate (characters / 4)
- Provider API (count after request)

**Recommendation:** tiktoken for accuracy

### Caching
**Options:**
- In-memory (Caffeine)
- Redis
- None (start simple)

**Recommendation:** Start without caching, add Redis if needed

### Summarization Strategy
**Options:**
1. Every N messages (e.g., 10)
2. Token-based threshold
3. Time-based (e.g., 1 hour)
4. Manual (user-triggered)

**Recommendation:** Combination of #1 and #2 (whichever comes first)

## 🐛 Known Issues / Design Decisions

### 1. Message TTL Implementation
**Problem:** PostgreSQL doesn't have native TTL like Redis.

**Options:**
- Scheduled job to delete expired messages
- Store in Redis with TTL, move to DB for summarization
- PostgreSQL pg_cron extension

**Recommendation:** Scheduled job (simplest)

### 2. Note De-duplication
**Problem:** How to detect if note already exists?

**Options:**
- Exact title match
- Fuzzy title match (Levenshtein distance)
- Semantic similarity (embeddings)

**Recommendation:** Start with fuzzy match, add embeddings later

### 3. Model Selection
**Problem:** When to use cheap vs premium models?

**Current logic:**
```
- intent="casual_chat" → cheap (gpt-3.5-turbo)
- intent="grammar_explanation" → premium (gpt-4)
- depth="brief" → cheap
- depth="detailed" → premium
- user.tier="free" + over budget → cheap or reject
```

**TODO:** Test and tune this logic based on cost/quality tradeoff

### 4. Context Budget Management
**Problem:** How much context to include?

**Strategy:**
- Reserve tokens for completion (~500)
- Reserve tokens for system prompt (~200)
- Remaining budget for context
- Include recent messages first (priority)
- Include summaries if budget allows
- Drop oldest summaries if needed

## 📊 Metrics to Track

### Cost Metrics
- Total tokens used (per user, per month)
- Cost per request
- Model distribution (cheap vs premium usage)

### Quality Metrics
- Note confidence scores (avg)
- Notes marked as userEdited (indicates quality issues)
- Review retention rates

### Performance Metrics
- LLM API latency (p50, p95, p99)
- Database query performance
- Endpoint response times

### User Metrics
- DAU/MAU
- Messages per user
- Notes per user
- Review completion rate
- Streak length

## 🚨 Alerts to Configure

1. **LLM API errors** > 5% (indicates provider issues)
2. **Token usage** approaching user limits
3. **High latency** on LLM calls (>5s)
4. **Database connection pool** exhaustion
5. **Failed summarization jobs**

## 🧪 Testing Strategy

### Unit Tests (Target: 80% coverage)
- All service methods
- SM-2 algorithm calculation
- Token counting
- JWT token generation/validation

### Integration Tests
- Auth flow (signup → login → protected endpoint)
- Chat flow (message → reply → note extraction)
- Review flow (queue → submit → next interval)

### Load Tests (Before production)
- 100 concurrent users
- 1000 messages/minute
- Verify no degradation

## 📝 Environment Variables

Required for production:

```bash
# Database
DB_USERNAME=
DB_PASSWORD=
DB_HOST=
DB_PORT=5432
DB_NAME=alang

# Security
JWT_SECRET=  # Generate with: openssl rand -base64 64

# LLM Provider
LLM_PROVIDER=openai
LLM_API_KEY=
LLM_API_BASE_URL=https://api.openai.com/v1

# Application
CORS_ORIGINS=https://your-frontend.com
SPRING_PROFILES_ACTIVE=prod
```

## 🎓 Learning Resources

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Security JWT](https://www.baeldung.com/spring-security-oauth-jwt)
- [OpenAI API Best Practices](https://platform.openai.com/docs/guides/production-best-practices)
- [Spaced Repetition (SM-2)](https://www.supermemo.com/en/archives1990-2015/english/ol/sm2)
- [Database Indexing](https://use-the-index-luke.com/)
