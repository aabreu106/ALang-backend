package com.alang.controller;

import com.alang.dto.note.CreateNoteRequest;
import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.entity.NoteType;
import com.alang.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteController noteController;

    @Test
    void getNotes_returnsOkWithNoteList() {
        NoteListResponse noteList = new NoteListResponse();
        noteList.setNotes(Collections.emptyList());
        noteList.setTotalCount(0);
        noteList.setPage(0);
        noteList.setPageSize(20);

        when(noteService.getNotes("user-1", "ja", null, null, null, null, null, 0, 20))
                .thenReturn(noteList);

        var response = noteController.getNotes("ja", null, null, null, null, null, 0, 20, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(noteList);
    }

    @Test
    void getNotes_passesAllFiltersToService() {
        NoteListResponse noteList = new NoteListResponse();
        when(noteService.getNotes("user-1", "ja", "vocab", 0.8, "kanji", null, null, 1, 10))
                .thenReturn(noteList);

        noteController.getNotes("ja", "vocab", 0.8, "kanji", null, null, 1, 10, "user-1");

        verify(noteService).getNotes("user-1", "ja", "vocab", 0.8, "kanji", null, null, 1, 10);
    }

    @Test
    void getNotes_passesTagFiltersToService() {
        NoteListResponse noteList = new NoteListResponse();
        when(noteService.getNotes("user-1", "ja", null, null, null, "topic", "food", 0, 20))
                .thenReturn(noteList);

        noteController.getNotes("ja", null, null, null, "topic", "food", 0, 20, "user-1");

        verify(noteService).getNotes("user-1", "ja", null, null, null, "topic", "food", 0, 20);
    }

    @Test
    void getTagValues_returnsOkWithValues() {
        when(noteService.getTagValues("user-1", "topic"))
                .thenReturn(List.of("food", "travel", "work"));

        var response = noteController.getTagValues("topic", "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("food", "travel", "work");
    }

    @Test
    void createNote_returnsCreatedStatus() {
        CreateNoteRequest request = new CreateNoteRequest();
        request.setType(NoteType.vocab);
        request.setLanguage("ja");
        request.setTitle("Kanji for water");
        request.setSummary("水 means water");
        request.setNoteContent("Detailed explanation...");

        NoteDto created = new NoteDto();
        created.setId("note-1");
        created.setType(NoteType.vocab);
        created.setTitle("Kanji for water");

        when(noteService.createNote(any(NoteDto.class), eq("user-1"))).thenReturn(created);

        var response = noteController.createNote(request, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
    }

    @Test
    void createNote_mapsRequestFieldsToNoteDto() {
        CreateNoteRequest request = new CreateNoteRequest();
        request.setType(NoteType.grammar);
        request.setLanguage("ja");
        request.setTitle("て-form");
        request.setSummary("Connecting form");
        request.setNoteContent("Full explanation");

        NoteDto created = new NoteDto();
        when(noteService.createNote(any(NoteDto.class), eq("user-1"))).thenReturn(created);

        noteController.createNote(request, "user-1");

        ArgumentCaptor<NoteDto> captor = ArgumentCaptor.forClass(NoteDto.class);
        verify(noteService).createNote(captor.capture(), eq("user-1"));

        NoteDto captured = captor.getValue();
        assertThat(captured.getType()).isEqualTo(NoteType.grammar);
        assertThat(captured.getLearningLanguage()).isEqualTo("ja");
        assertThat(captured.getTitle()).isEqualTo("て-form");
        assertThat(captured.getSummary()).isEqualTo("Connecting form");
        assertThat(captured.getNoteContent()).isEqualTo("Full explanation");
        assertThat(captured.getUserEdited()).isTrue();
    }

    @Test
    void getNote_returnsOkWithNote() {
        NoteDto note = new NoteDto();
        note.setId("note-1");
        note.setTitle("Test Note");

        when(noteService.getNote("note-1", "user-1")).thenReturn(note);

        var response = noteController.getNote("note-1", "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(note);
    }

    @Test
    void updateNote_returnsOkWithUpdatedNote() {
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitle("Updated Title");
        request.setSummary("Updated summary");

        NoteDto updated = new NoteDto();
        updated.setId("note-1");
        updated.setTitle("Updated Title");

        when(noteService.updateNote("note-1", request, "user-1", true)).thenReturn(updated);

        var response = noteController.updateNote("note-1", request, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
    }

    @Test
    void deleteNote_returnsNoContent() {
        doNothing().when(noteService).deleteNote("note-1", "user-1");

        var response = noteController.deleteNote("note-1", "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(noteService).deleteNote("note-1", "user-1");
    }
}
