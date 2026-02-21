package com.alang.service.impl;

import com.alang.dto.note.*;
import com.alang.entity.*;
import com.alang.exception.NoteNotFoundException;
import com.alang.exception.UnauthorizedException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.NoteRepository;
import com.alang.repository.NoteTagRepository;
import com.alang.repository.UserRepository;
import com.alang.service.NoteService;
import jakarta.persistence.EntityManager;
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
    private final NoteTagRepository noteTagRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final EntityManager entityManager;

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
        note.setStructuredContent(noteDto.getStructuredContent());
        note.setUserEdited(false);

        // Save note first to get ID, then add tags
        Note saved = noteRepository.save(note);

        // Create tags if provided
        if (noteDto.getTags() != null && !noteDto.getTags().isEmpty()) {
            for (NoteTagDto tagDto : noteDto.getTags()) {
                NoteTag tag = new NoteTag();
                tag.setNote(saved);
                tag.setTagCategory(tagDto.getCategory());
                tag.setTagValue(tagDto.getValue());
                saved.getTags().add(tag);
            }
            saved = noteRepository.save(saved);
        }

        log.info("Created note: id={}, type={}, title={}, tags={}, userId={}",
                saved.getId(), saved.getType(), saved.getTitle(), saved.getTags().size(), userId);
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

    // Paginated retrieval with optional filtering by language, type, tags, and search query.
    @Override
    public NoteListResponse getNotes(String userId, String language, String type,
                                     Double minConfidence, String searchQuery,
                                     String tagCategory, String tagValue,
                                     int page, int pageSize) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Note> notePage;

        if (searchQuery != null && !searchQuery.isBlank()) {
            notePage = noteRepository.searchNotes(user, searchQuery.trim(), pageRequest);
        } else if (tagCategory != null && tagValue != null) {
            // Tag-based filtering
            if (language != null) {
                Language lang = languageRepository.findById(language).orElse(null);
                if (lang == null) {
                    return emptyResponse(page, pageSize);
                }
                notePage = noteRepository.findByUserAndLearningLanguageAndTag(user, lang, tagCategory, tagValue, pageRequest);
            } else {
                notePage = noteRepository.findByUserAndTag(user, tagCategory, tagValue, pageRequest);
            }
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

    @Override
    public List<String> getTagValues(String userId, String category) {
        return noteTagRepository.findDistinctTagValuesByUserAndCategory(userId, category);
    }

    // Partial update (title/summary/content). Pass markAsUserEdited=true for user edits, false for LLM-driven updates.
    @Override
    @Transactional
    public NoteDto updateNote(String noteId, UpdateNoteRequest updateRequest, String userId, boolean markAsUserEdited) {
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
        if (updateRequest.getStructuredContent() != null) {
            note.setStructuredContent(updateRequest.getStructuredContent());
        }
        // Replace tags if provided
        if (updateRequest.getTags() != null) {
            note.getTags().clear();
            // Flush to execute orphan DELETEs before INSERTs, avoiding unique constraint violations
            // when new tags have the same (note_id, category, value) as old ones.
            entityManager.flush();
            for (NoteTagDto tagDto : updateRequest.getTags()) {
                NoteTag tag = new NoteTag();
                tag.setNote(note);
                tag.setTagCategory(tagDto.getCategory());
                tag.setTagValue(tagDto.getValue());
                note.getTags().add(tag);
            }
        }

        note.setUserEdited(markAsUserEdited);

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
        dto.setStructuredContent(note.getStructuredContent());
        dto.setUserEdited(note.getUserEdited());
        dto.setReviewCount(note.getReviewCount());
        dto.setLastReviewedAt(note.getLastReviewedAt());
        dto.setNextReviewAt(note.getNextReviewAt());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());

        // Map tags
        if (note.getTags() != null) {
            dto.setTags(note.getTags().stream()
                    .map(t -> new NoteTagDto(t.getTagCategory(), t.getTagValue()))
                    .toList());
        }

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
