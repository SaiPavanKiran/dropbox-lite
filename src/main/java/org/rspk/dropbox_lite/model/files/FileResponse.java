package org.rspk.dropbox_lite.model.files;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.rspk.dropbox_lite.model.common.TemporalRes;

import java.util.UUID;

public record FileResponse (
        UUID fileId,
        UUID folderId,
        String name,
        String contentType,
        Long size,
        Boolean archived,
        UploadStatus uploadStatus,
        @JsonUnwrapped
        TemporalRes timestamps
) {}
