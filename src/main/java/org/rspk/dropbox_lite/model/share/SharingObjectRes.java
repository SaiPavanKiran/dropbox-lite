package org.rspk.dropbox_lite.model.share;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record SharingObjectRes(
        UUID objectId,
        String name,
        String recipientEmail,
        boolean isActivelySharing,
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant sharedAt,
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant updatedReqAt,
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant sharedUntil
) {}
