package com.alang.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageEntityTest {

    @Test
    void defaultFullySupported_isTrue() {
        Language lang = new Language();
        assertThat(lang.isFullySupported()).isTrue();
    }

    @Test
    void fieldsAreSetAndRetrievable() {
        Language lang = new Language();
        lang.setCode("ja");
        lang.setName("Japanese");
        lang.setNativeName("日本語");

        assertThat(lang.getCode()).isEqualTo("ja");
        assertThat(lang.getName()).isEqualTo("Japanese");
        assertThat(lang.getNativeName()).isEqualTo("日本語");
    }
}
