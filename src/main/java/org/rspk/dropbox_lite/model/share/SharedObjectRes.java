package org.rspk.dropbox_lite.model.share;


import java.time.Instant;
import java.util.UUID;

public record SharedObjectRes (
        UUID objectId,
        String name,
        String link,
        Instant linkExpiredAt
){}
