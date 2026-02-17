package com.alang.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LLMPropertiesTest {

    @Test
    void defaultValues_areInitialized() {
        LLMProperties properties = new LLMProperties();

        assertThat(properties.getApi()).isNotNull();
        assertThat(properties.getModels()).isNotNull();
        assertThat(properties.getTokenLimits()).isNotNull();
        assertThat(properties.getSummarization()).isNotNull();
    }

    @Test
    void provider_canBeSetAndRetrieved() {
        LLMProperties properties = new LLMProperties();
        properties.setProvider("openai");

        assertThat(properties.getProvider()).isEqualTo("openai");
    }

    @Test
    void apiProperties_canBeSetAndRetrieved() {
        LLMProperties.Api api = new LLMProperties.Api();
        api.setKey("sk-test-key");
        api.setBaseUrl("https://api.openai.com/v1");

        assertThat(api.getKey()).isEqualTo("sk-test-key");
        assertThat(api.getBaseUrl()).isEqualTo("https://api.openai.com/v1");
    }

    @Test
    void modelProperties_canBeSetAndRetrieved() {
        LLMProperties.Models models = new LLMProperties.Models();
        models.setCheap("gpt-3.5-turbo");
        models.setStandard("gpt-4-turbo");
        models.setPremium("gpt-4");

        assertThat(models.getCheap()).isEqualTo("gpt-3.5-turbo");
        assertThat(models.getStandard()).isEqualTo("gpt-4-turbo");
        assertThat(models.getPremium()).isEqualTo("gpt-4");
    }

    @Test
    void tokenLimits_canBeSetAndRetrieved() {
        LLMProperties.TokenLimits limits = new LLMProperties.TokenLimits();
        limits.setFreeTierDaily(3500);
        limits.setProTierDaily(35000);
        limits.setPerRequestMax(3500);

        assertThat(limits.getFreeTierDaily()).isEqualTo(3500);
        assertThat(limits.getProTierDaily()).isEqualTo(35000);
        assertThat(limits.getPerRequestMax()).isEqualTo(3500);
    }

    @Test
    void summarization_canBeSetAndRetrieved() {
        LLMProperties.Summarization summarization = new LLMProperties.Summarization();
        summarization.setMessageThreshold(10);
        summarization.setTokenThreshold(2000);

        assertThat(summarization.getMessageThreshold()).isEqualTo(10);
        assertThat(summarization.getTokenThreshold()).isEqualTo(2000);
    }
}
