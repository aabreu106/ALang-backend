package com.alang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {

    private String provider;
    private Api api = new Api();
    private Models models = new Models();
    private TokenLimits tokenLimits = new TokenLimits();
    private Summarization summarization = new Summarization();

    @Data
    public static class Api {
        private String key;
        private String baseUrl;
    }

    @Data
    public static class Models {
        private String cheap;
        private String standard;
        private String premium;
    }

    @Data
    public static class TokenLimits {
        private int freeTierDaily;
        private int proTierDaily;
        private int perRequestMax;
    }

    @Data
    public static class Summarization {
        private int messageThreshold;
        private int tokenThreshold;
    }
}
