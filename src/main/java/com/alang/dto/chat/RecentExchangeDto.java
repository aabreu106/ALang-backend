package com.alang.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * A very recent message exchange (user + assistant).
 * These are kept temporarily (e.g., 24 hours) for immediate context.
 *
 * ARCHITECTURAL NOTE:
 * - Should have TTL (time-to-live) in database
 * - After TTL expires, these are summarized and deleted
 * - This prevents unbounded growth of chat history
 */
@Data
public class RecentExchangeDto {
    private String userMessage;
    private String assistantReply;
    private LocalDateTime timestamp;

    /**
     * TODO: Implement TTL mechanism
     * - Option 1: Database-level TTL (PostgreSQL doesn't have native TTL, use pg_cron)
     * - Option 2: Application-level cleanup job
     * - Option 3: Store in Redis with TTL, then move to summary
     */
}
