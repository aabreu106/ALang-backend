package com.alang.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token usage information for transparency and cost control.
 *
 * ARCHITECTURAL NOTE:
 * - This should be tracked in LLMService
 * - Can be used for rate limiting, cost alerts, analytics
 * - In production, persist this to a separate table for reporting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageDto {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    /**
     * Estimated cost in USD (optional)
     * Calculated based on model pricing
     */
    private Double estimatedCost;
}
