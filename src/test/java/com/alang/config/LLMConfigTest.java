package com.alang.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class LLMConfigTest {

    @Test
    void llmWebClient_returnsConfiguredWebClient() {
        LLMProperties properties = new LLMProperties();
        LLMProperties.Api api = new LLMProperties.Api();
        api.setKey("test-key");
        api.setBaseUrl("https://api.example.com/v1");
        properties.setApi(api);

        LLMConfig config = new LLMConfig(properties);
        WebClient webClient = config.llmWebClient();

        assertThat(webClient).isNotNull();
    }

    @Test
    void llmWebClient_withDifferentBaseUrl_returnsWebClient() {
        LLMProperties properties = new LLMProperties();
        LLMProperties.Api api = new LLMProperties.Api();
        api.setKey("another-key");
        api.setBaseUrl("http://localhost:11434/v1");
        properties.setApi(api);

        LLMConfig config = new LLMConfig(properties);
        WebClient webClient = config.llmWebClient();

        assertThat(webClient).isNotNull();
    }
}
