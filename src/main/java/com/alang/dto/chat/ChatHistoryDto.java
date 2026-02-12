package com.alang.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for GET /chat/history
 *
 * CRITICAL ARCHITECTURAL NOTE:
 * - This is NOT raw chat messages
 * - This is SUMMARIZED conversation context
 * - Raw messages are discarded after summarization
 *
 * Why?
 * 1. Token limits: Can't replay 1000 messages into context
 * 2. Cost control: Summaries are cheaper to process
 * 3. Privacy: Old detailed messages can be purged
 * 4. Performance: Summaries are faster to load
 */
@Data
public class ChatHistoryDto {
    private String language;

    /**
     * Recent conversation summaries (NOT individual messages)
     * Example: "User asked about は vs が. Explanation provided with examples."
     */
    private List<ConversationSummaryDto> summaries;

    /**
     * Very recent exchanges (last 5-10) MAY be kept for immediate context
     * But these should be stored with expiration (e.g., 24 hours)
     */
    private List<RecentExchangeDto> recentExchanges;

    private LocalDateTime oldestSummary;
    private LocalDateTime newestSummary;
}
