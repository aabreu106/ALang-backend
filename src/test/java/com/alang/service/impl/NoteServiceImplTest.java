package com.alang.service.impl;

import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
import com.alang.dto.note.NoteTagDto;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.entity.Language;
import com.alang.entity.Note;
import com.alang.entity.NoteType;
import com.alang.entity.User;
import com.alang.exception.NoteNotFoundException;
import com.alang.exception.UnauthorizedException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.NoteRepository;
import com.alang.repository.NoteTagRepository;
import com.alang.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteTagRepository noteTagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private NoteServiceImpl noteService;

    private User testUser;
    private Language english;
    private Language japanese;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setAppLanguageCode("en");

        english = new Language();
        english.setCode("en");
        english.setName("English");
        english.setNativeName("English");

        japanese = new Language();
        japanese.setCode("ja");
        japanese.setName("Japanese");
        japanese.setNativeName("日本語");
    }

    private Note createTestNote(String id, String title) {
        Note note = new Note();
        note.setId(id);
        note.setUser(testUser);
        note.setTeachingLanguage(english);
        note.setLearningLanguage(japanese);
        note.setType(NoteType.vocab);
        note.setTitle(title);
        note.setSummary("A summary");
        note.setNoteContent("Full content");
        note.setUserEdited(false);
        note.setReviewCount(0);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }

    // --- createNote ---

    @Test
    void createNote_savesAndReturnsDto() {
        NoteDto input = new NoteDto();
        input.setType(NoteType.vocab);
        input.setLearningLanguage("ja");
        input.setTitle("水");
        input.setSummary("Water");
        input.setNoteContent("Detailed explanation");

        Note saved = createTestNote("note-1", "水");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteDto result = noteService.createNote(input, "user-1");

        assertThat(result.getId()).isEqualTo("note-1");
        assertThat(result.getTitle()).isEqualTo("水");
        assertThat(result.getType()).isEqualTo(NoteType.vocab);
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void createNote_savesWithStructuredContentAndTags() {
        NoteDto input = new NoteDto();
        input.setType(NoteType.vocab);
        input.setLearningLanguage("ja");
        input.setTitle("水");
        input.setStructuredContent(Map.of("word", "水", "meaning", "water", "partOfSpeech", "noun"));
        input.setTags(List.of(
                new NoteTagDto("topic", "daily_life"),
                new NoteTagDto("difficulty", "beginner")
        ));

        Note saved = createTestNote("note-1", "水");
        saved.setStructuredContent(Map.of("word", "水", "meaning", "water", "partOfSpeech", "noun"));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteDto result = noteService.createNote(input, "user-1");

        assertThat(result.getStructuredContent()).containsEntry("word", "水");
        // save called twice: once for note, once after adding tags
        verify(noteRepository, times(2)).save(any(Note.class));
    }

    @Test
    void createNote_usesUserAppLanguageWhenTeachingLanguageNotProvided() {
        NoteDto input = new NoteDto();
        input.setType(NoteType.grammar);
        input.setLearningLanguage("ja");
        input.setTitle("て-form");

        Note saved = createTestNote("note-1", "て-form");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        noteService.createNote(input, "user-1");

        verify(languageRepository).findById("en"); // fallback to app language
    }

    @Test
    void createNote_throwsWhenUserNotFound() {
        NoteDto input = new NoteDto();
        input.setLearningLanguage("ja");
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(input, "missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createNote_throwsWhenLanguageNotSupported() {
        NoteDto input = new NoteDto();
        input.setLearningLanguage("xx");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("xx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(input, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Language not supported");
    }

    // --- createNotes (batch with de-duplication) ---

    @Test
    void createNotes_skipesDuplicatesAndReturnsExisting() {
        NoteDto note1 = new NoteDto();
        note1.setType(NoteType.vocab);
        note1.setLearningLanguage("ja");
        note1.setTitle("水");

        NoteDto note2 = new NoteDto();
        note2.setType(NoteType.vocab);
        note2.setLearningLanguage("ja");
        note2.setTitle("火");

        Note existingNote = createTestNote("existing-1", "水");
        Note newNote = createTestNote("new-1", "火");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));

        // "水" already exists → duplicate
        when(noteRepository.findByUserAndLearningLanguageAndTitleIgnoreCase(testUser, japanese, "水"))
                .thenReturn(List.of(existingNote));
        // "火" is new
        when(noteRepository.findByUserAndLearningLanguageAndTitleIgnoreCase(testUser, japanese, "火"))
                .thenReturn(List.of());
        when(noteRepository.save(any(Note.class))).thenReturn(newNote);

        List<NoteDto> result = noteService.createNotes(List.of(note1, note2), "user-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("existing-1"); // duplicate returned existing
        assertThat(result.get(1).getId()).isEqualTo("new-1"); // new was created
        verify(noteRepository, times(1)).save(any(Note.class)); // only 1 save for the new note
    }

    // --- getNote ---

    @Test
    void getNote_returnsDtoWhenOwner() {
        Note note = createTestNote("note-1", "Test");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        NoteDto result = noteService.getNote("note-1", "user-1");

        assertThat(result.getId()).isEqualTo("note-1");
    }

    @Test
    void getNote_throwsWhenNoteNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNote("missing", "user-1"))
                .isInstanceOf(NoteNotFoundException.class);
    }

    @Test
    void getNote_throwsWhenNotOwner() {
        User otherUser = new User();
        otherUser.setId("other-user");

        Note note = createTestNote("note-1", "Test");
        note.setUser(otherUser);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.getNote("note-1", "user-1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- getNotes ---

    @Test
    void getNotes_returnsAllUserNotes() {
        Note note = createTestNote("note-1", "Test");
        Page<Note> page = new PageImpl<>(List.of(note));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByUser(eq(testUser), any(Pageable.class))).thenReturn(page);

        NoteListResponse result = noteService.getNotes("user-1", null, null, null, null, null, null, 0, 20);

        assertThat(result.getNotes()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    void getNotes_filtersbyLanguageAndType() {
        Page<Note> page = new PageImpl<>(List.of());

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(noteRepository.findByUserAndLearningLanguageAndType(
                eq(testUser), eq(japanese), eq(NoteType.vocab), any(Pageable.class)))
                .thenReturn(page);

        NoteListResponse result = noteService.getNotes("user-1", "ja", "vocab", null, null, null, null, 0, 20);

        assertThat(result.getNotes()).isEmpty();
        verify(noteRepository).findByUserAndLearningLanguageAndType(
                eq(testUser), eq(japanese), eq(NoteType.vocab), any(Pageable.class));
    }

    @Test
    void getNotes_filtersByTag() {
        Note note = createTestNote("note-1", "水");
        Page<Note> page = new PageImpl<>(List.of(note));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByUserAndTag(eq(testUser), eq("topic"), eq("food"), any(Pageable.class)))
                .thenReturn(page);

        NoteListResponse result = noteService.getNotes("user-1", null, null, null, null, "topic", "food", 0, 20);

        assertThat(result.getNotes()).hasSize(1);
        verify(noteRepository).findByUserAndTag(eq(testUser), eq("topic"), eq("food"), any(Pageable.class));
    }

    @Test
    void getNotes_filtersByLanguageAndTag() {
        Page<Note> page = new PageImpl<>(List.of());

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(noteRepository.findByUserAndLearningLanguageAndTag(
                eq(testUser), eq(japanese), eq("difficulty"), eq("beginner"), any(Pageable.class)))
                .thenReturn(page);

        NoteListResponse result = noteService.getNotes("user-1", "ja", null, null, null, "difficulty", "beginner", 0, 20);

        verify(noteRepository).findByUserAndLearningLanguageAndTag(
                eq(testUser), eq(japanese), eq("difficulty"), eq("beginner"), any(Pageable.class));
    }

    @Test
    void getNotes_searchesByQuery() {
        Page<Note> page = new PageImpl<>(List.of());

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.searchNotes(eq(testUser), eq("kanji"), any(Pageable.class))).thenReturn(page);

        NoteListResponse result = noteService.getNotes("user-1", null, null, null, "kanji", null, null, 0, 20);

        verify(noteRepository).searchNotes(eq(testUser), eq("kanji"), any(Pageable.class));
    }

    @Test
    void getNotes_returnsEmptyWhenLanguageNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("xx")).thenReturn(Optional.empty());

        NoteListResponse result = noteService.getNotes("user-1", "xx", null, null, null, null, null, 0, 20);

        assertThat(result.getNotes()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
    }

    @Test
    void getNotes_returnsEmptyWhenInvalidType() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));

        NoteListResponse result = noteService.getNotes("user-1", "ja", "invalid_type", null, null, null, null, 0, 20);

        assertThat(result.getNotes()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
    }

    // --- getTagValues ---

    @Test
    void getTagValues_returnDistinctValues() {
        when(noteTagRepository.findDistinctTagValuesByUserAndCategory("user-1", "topic"))
                .thenReturn(List.of("food", "travel", "work"));

        List<String> result = noteService.getTagValues("user-1", "topic");

        assertThat(result).containsExactly("food", "travel", "work");
    }

    // --- updateNote ---

    @Test
    void updateNote_updatesFieldsAndSetsUserEdited() {
        Note note = createTestNote("note-1", "Old Title");
        note.setSummary("Old summary");

        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setTitle("New Title");
        update.setSummary("New summary");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteDto result = noteService.updateNote("note-1", update, "user-1", true);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getSummary()).isEqualTo("New summary");
        assertThat(result.getUserEdited()).isTrue();
    }

    @Test
    void updateNote_onlyUpdatesProvidedFields() {
        Note note = createTestNote("note-1", "Original");
        note.setSummary("Original summary");
        note.setNoteContent("Original content");

        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setTitle("Updated Title");
        // summary and noteContent are null → should not change

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteDto result = noteService.updateNote("note-1", update, "user-1", true);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getSummary()).isEqualTo("Original summary");
        assertThat(result.getNoteContent()).isEqualTo("Original content");
    }

    @Test
    void updateNote_replacesTagsWhenProvided() {
        Note note = createTestNote("note-1", "水");

        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setTags(List.of(
                new NoteTagDto("topic", "food"),
                new NoteTagDto("difficulty", "beginner")
        ));

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteDto result = noteService.updateNote("note-1", update, "user-1", true);

        assertThat(result.getTags()).hasSize(2);
        assertThat(result.getUserEdited()).isTrue();
    }

    @Test
    void updateNote_throwsWhenNotOwner() {
        User otherUser = new User();
        otherUser.setId("other-user");

        Note note = createTestNote("note-1", "Test");
        note.setUser(otherUser);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.updateNote("note-1", new UpdateNoteRequest(), "user-1", true))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- deleteNote ---

    @Test
    void deleteNote_deletesWhenOwner() {
        Note note = createTestNote("note-1", "Test");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        noteService.deleteNote("note-1", "user-1");

        verify(noteRepository).delete(note);
    }

    @Test
    void deleteNote_throwsWhenNotOwner() {
        User otherUser = new User();
        otherUser.setId("other-user");

        Note note = createTestNote("note-1", "Test");
        note.setUser(otherUser);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.deleteNote("note-1", "user-1"))
                .isInstanceOf(UnauthorizedException.class);

        verify(noteRepository, never()).delete(any());
    }

    @Test
    void deleteNote_throwsWhenNoteNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote("missing", "user-1"))
                .isInstanceOf(NoteNotFoundException.class);
    }

    // --- findSimilarNotes ---

    @Test
    void findSimilarNotes_returnsMatchingNotes() {
        NoteDto input = new NoteDto();
        input.setLearningLanguage("ja");
        input.setTitle("水");

        Note match = createTestNote("existing-1", "水");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(noteRepository.findByUserAndLearningLanguageAndTitleIgnoreCase(testUser, japanese, "水"))
                .thenReturn(List.of(match));

        List<NoteDto> result = noteService.findSimilarNotes(input, "user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("水");
    }

    @Test
    void findSimilarNotes_returnsEmptyWhenLanguageNotFound() {
        NoteDto input = new NoteDto();
        input.setLearningLanguage("xx");
        input.setTitle("test");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("xx")).thenReturn(Optional.empty());

        List<NoteDto> result = noteService.findSimilarNotes(input, "user-1");

        assertThat(result).isEmpty();
    }
}
