package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FilesArchiveReq(
        List<UUID> fileIds,
        List<UUID> folderIds,
        @NotNull(message = "can't accept null for boolean")
        Boolean allFiles
) {}
