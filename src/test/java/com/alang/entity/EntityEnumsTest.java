package com.alang.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that enum constants remain stable.
 * LLMServiceImpl's VALID_NOTE_TYPES set and string-based validation depend on
 * these exact names, so regressions here would silently break note parsing.
 */
class EntityEnumsTest {

    // ---- NoteType ----

    @Test
    void noteType_hasExpectedValues() {
        assertThat(NoteType.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("vocab", "grammar", "exception", "other");
    }

    @Test
    void noteType_parsedFromString() {
        assertThat(NoteType.valueOf("vocab")).isEqualTo(NoteType.vocab);
        assertThat(NoteType.valueOf("grammar")).isEqualTo(NoteType.grammar);
        assertThat(NoteType.valueOf("exception")).isEqualTo(NoteType.exception);
        assertThat(NoteType.valueOf("other")).isEqualTo(NoteType.other);
    }

    // ---- UserTier ----

    @Test
    void userTier_hasExpectedValues() {
        assertThat(UserTier.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("free", "pro");
    }

    // ---- RoleType ----

    @Test
    void roleType_hasExpectedValues() {
        assertThat(RoleType.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("user", "assistant");
    }

    @Test
    void roleType_nameMatchesMessageContextKey() {
        // ChatServiceImpl uses role.name() as the "role" key sent to LLM
        assertThat(RoleType.user.name()).isEqualTo("user");
        assertThat(RoleType.assistant.name()).isEqualTo("assistant");
    }

    // ---- SessionStatus ----

    @Test
    void sessionStatus_hasExpectedValues() {
        assertThat(SessionStatus.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("active", "closed");
    }
}
