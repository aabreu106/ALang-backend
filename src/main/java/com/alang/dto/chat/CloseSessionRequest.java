package com.alang.dto.chat;

import lombok.Data;

/**
 * Request body for POST /chat/sessions/{sessionId}/close.
 *
 * If force=false (default), the backend will check whether a note has been created
 * from the session before closing it. If no note exists, the response will include
 * noteCreated=false and the session will NOT be closed, allowing the frontend to
 * prompt the user for confirmation.
 *
 * If force=true, the check is skipped and the session is closed immediately.
 */
@Data
public class CloseSessionRequest {

    /**
     * When true, skip the note-created check and close the session unconditionally.
     * Used after the user confirms they want to close without saving a note.
     */
    private boolean force = false;
}
