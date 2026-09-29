package org.rspk.dropbox_lite.model.folders;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.rspk.dropbox_lite.model.common.TemporalRes;

import java.util.UUID;

public record FolderRes (
        UUID folderId,
        UUID parentFolderId,
        String name,
        @JsonUnwrapped
        TemporalRes timestamps
) {}
