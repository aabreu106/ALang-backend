package com.alang.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetaControllerTest {

    private final MetaController metaController = new MetaController();

    @Test
    void getLanguages_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> metaController.getLanguages())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getStarterPrompts_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> metaController.getStarterPrompts("ja"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
