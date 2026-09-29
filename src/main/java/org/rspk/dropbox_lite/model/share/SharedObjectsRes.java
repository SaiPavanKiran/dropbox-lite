package org.rspk.dropbox_lite.model.share;

import java.time.Instant;
import java.util.UUID;

public record SharedObjectsRes (
        String ownerEmail,
        UUID objectId,
        String name,
        Instant sharedAt,
        Instant updatedReqAt,
        Instant sharedUntil
) {}
