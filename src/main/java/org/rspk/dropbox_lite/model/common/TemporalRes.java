package org.rspk.dropbox_lite.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record TemporalRes (
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant createdAt,
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant updatedAt
) {}
