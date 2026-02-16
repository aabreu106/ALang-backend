package com.alang.service.impl;

import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
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
import com.alang.repository.UserRepository;
import com.alang.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;

    // Persist a single note, defaulting teachingLanguage to the user's app language if not provided.
    @Override
    @Transactional
    public NoteDto createNote(NoteDto noteDto, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Language teachingLanguage = noteDto.getTeachingLanguage() != null
                ? languageRepository.findById(noteDto.getTeachingLanguage()).orElse(null)
                : null;
        Language learningLanguage = languageRepository.findById(noteDto.getLearningLanguage())
                .orElseThrow(() -> new IllegalArgumentException("Language not supported: " + noteDto.getLearningLanguage()));

        // If teaching language not provided, use user's app language
        if (teachingLanguage == null) {
            teachingLanguage = languageRepository.findById(user.getAppLanguageCode())
                    .orElseThrow(() -> new IllegalStateException("App language not found: " + user.getAppLanguageCode()));
        }

        Note note = new Note();
        note.setUser(user);
        note.setTeachingLanguage(teachingLanguage);
        note.setLearningLanguage(learningLanguage);
        note.setType(noteDto.getType());
        note.setTitle(noteDto.getTitle());
        note.setSummary(noteDto.getSummary());
        note.setNoteContent(noteDto.getNoteContent());
        note.setUserEdited(false);

        Note saved = noteRepository.save(note);
        log.info("Created note: id={}, type={}, title={}, userId={}", saved.getId(), saved.getType(), saved.getTitle(), userId);
        return toDto(saved);
    }

    // Batch creation with de-duplication: checks findSimilarNotes() before each insert, skips duplicates.
    @Override
    @Transactional
    public List<NoteDto> createNotes(List<NoteDto> notes, String userId) {
        List<NoteDto> created = new ArrayList<>();
        for (NoteDto noteDto : notes) {
            // Check for duplicates before creating
            List<NoteDto> similar = findSimilarNotes(noteDto, userId);
            if (similar.isEmpty()) {
                created.add(createNote(noteDto, userId));
            } else {
                log.info("Skipping duplicate note: title='{}', existingId={}", noteDto.getTitle(), similar.get(0).getId());
                created.add(similar.get(0));
            }
        }
        return created;
    }

    // Retrieve a single note by ID with ownership verification.
    @Override
    public NoteDto getNote(String noteId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        if (!note.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this note");
        }

        return toDto(note);
    }

    // Paginated retrieval with optional filtering by language, type, and search query.
    @Override
    public NoteListResponse getNotes(String userId, String language, String type,
                                     Double minConfidence, String searchQuery,
                                     int page, int pageSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Note> notePage;

        if (searchQuery != null && !searchQuery.isBlank()) {
            notePage = noteRepository.searchNotes(user, searchQuery.trim(), pageRequest);
        } else if (language != null && type != null) {
            Language lang = languageRepository.findById(language).orElse(null);
            if (lang == null) {
                return emptyResponse(page, pageSize);
            }
            NoteType noteType;
            try {
                noteType = NoteType.valueOf(type);
            } catch (IllegalArgumentException e) {
                return emptyResponse(page, pageSize);
            }
            notePage = noteRepository.findByUserAndLearningLanguageAndType(user, lang, noteType, pageRequest);
        } else if (language != null) {
            Language lang = languageRepository.findById(language).orElse(null);
            if (lang == null) {
                return emptyResponse(page, pageSize);
            }
            notePage = noteRepository.findByUserAndLearningLanguage(user, lang, pageRequest);
        } else {
            notePage = noteRepository.findByUser(user, pageRequest);
        }

        NoteListResponse response = new NoteListResponse();
        response.setNotes(notePage.getContent().stream().map(this::toDto).toList());
        response.setTotalCount((int) notePage.getTotalElements());
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    // Partial update (title/summary/content). Sets userEdited=true to mark this note as manually curated.
    @Override
    @Transactional
    public NoteDto updateNote(String noteId, UpdateNoteRequest updateRequest, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        if (!note.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this note");
        }

        if (updateRequest.getTitle() != null) {
            note.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getSummary() != null) {
            note.setSummary(updateRequest.getSummary());
        }
        if (updateRequest.getNoteContent() != null) {
            note.setNoteContent(updateRequest.getNoteContent());
        }

        note.setUserEdited(true);

        Note saved = noteRepository.save(note);
        log.info("Updated note: id={}, userId={}", noteId, userId);
        return toDto(saved);
    }

    // Delete a note with ownership verification.
    @Override
    @Transactional
    public void deleteNote(String noteId, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        if (!note.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not have access to this note");
        }

        noteRepository.delete(note);
        log.info("Deleted note: id={}, userId={}", noteId, userId);
    }

    // Case-insensitive exact title match per user+language for de-duplication.
    @Override
    public List<NoteDto> findSimilarNotes(NoteDto noteDto, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Language learningLanguage = languageRepository.findById(noteDto.getLearningLanguage()).orElse(null);
        if (learningLanguage == null) {
            return List.of();
        }

        List<Note> matches = noteRepository.findByUserAndLearningLanguageAndTitleIgnoreCase(
                user, learningLanguage, noteDto.getTitle());

        return matches.stream().map(this::toDto).toList();
    }

    // ---- Helpers ----

    // Map Note entity to NoteDto for API responses.
    private NoteDto toDto(Note note) {
        NoteDto dto = new NoteDto();
        dto.setId(note.getId());
        dto.setType(note.getType());
        dto.setTeachingLanguage(note.getTeachingLanguage().getCode());
        dto.setLearningLanguage(note.getLearningLanguage().getCode());
        dto.setTitle(note.getTitle());
        dto.setSummary(note.getSummary());
        dto.setNoteContent(note.getNoteContent());
        dto.setUserEdited(note.getUserEdited());
        dto.setReviewCount(note.getReviewCount());
        dto.setLastReviewedAt(note.getLastReviewedAt());
        dto.setNextReviewAt(note.getNextReviewAt());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        return dto;
    }

    private NoteListResponse emptyResponse(int page, int pageSize) {
        NoteListResponse response = new NoteListResponse();
        response.setNotes(List.of());
        response.setTotalCount(0);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }
}
