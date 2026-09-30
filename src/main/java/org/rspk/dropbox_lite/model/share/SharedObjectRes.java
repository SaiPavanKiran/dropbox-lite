package org.rspk.dropbox_lite.model.share;


import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record SharedObjectRes (
        UUID objectId,
        String name,
        String link,
        @JsonFormat(
                pattern = "uuuu-MM-dd'T'HH:mm:ss",
                timezone = "UTC"
        )
        Instant linkExpiredAt
){}
