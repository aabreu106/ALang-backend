package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Condensed conversation summary.
 *
 * CRITICAL ARCHITECTURAL NOTE:
 * This is THE solution to the "chat history explosion" problem.
 *
 * Why summaries instead of raw messages?
 * 1. TOKEN LIMITS: Can't send 1000 messages to LLM as context
 * 2. COST CONTROL: Summaries are 10-20x cheaper to process
 * 3. PRIVACY: Can purge old detailed messages, keep summaries
 * 4. PERFORMANCE: Faster to load and process
 *
 * How it works:
 * - Every N messages (e.g., 10), LLMService generates a summary
 * - Summary condenses 10 exchanges into 1-2 paragraphs
 * - Original messages can be deleted after summarization
 * - When user sends new message, include recent summaries as context (not old messages)
 *
 * Example:
 * - Messages 1-10: User asks about Japanese particles
 * - Summary: "User explored は、が、を、に. Understood topic vs subject distinction. Still confused about を vs に."
 * - Messages 11-20: User asks about verb conjugation
 * - Summary: "User learned -ます form conjugation. Practiced with る、う、く verbs."
 * - Message 21: User asks "Can you remind me about は?"
 * - Context sent to LLM: [Summary of 1-10] + [Summary of 11-20] + [Message 21]
 *
 * TODO: Implement auto-summarization in ChatService
 * TODO: Implement cleanup job to delete old raw messages after summarization
 */
@Entity
@Table(name = "conversation_summaries")
@Data
public class ConversationSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "language_code", nullable = false)
    private Language language;

    /**
     * The condensed summary text (1-3 paragraphs)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    /**
     * Key topics covered (for quick search/filtering)
     */
    @ElementCollection
    @CollectionTable(name = "summary_topics")
    private java.util.List<String> topics;

    /**
     * How many actual message exchanges does this summary represent?
     * Useful for analytics and threshold tuning
     */
    @Column(nullable = false)
    private Integer messageCount;

    /**
     * Token count of the summary itself
     * Used for context budget management
     */
    private Integer summaryTokenCount;

    /**
     * When was this summary created?
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Time range this summary covers
     */
    private LocalDateTime conversationStartTime;
    private LocalDateTime conversationEndTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * TODO: Implement summarization strategy
     * Options:
     * 1. Every N messages, trigger summarization
     * 2. Time-based (every 1 hour of conversation)
     * 3. Token-based (when context exceeds X tokens)
     * 4. Semantic-based (when topic changes significantly)
     *
     * Recommended: Combination of #1 and #3
     */
}
