package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FilesArchiveReq(
        List<@org.hibernate.validator.constraints.UUID(message = "not a valid id") String> fileIds,
        List<@org.hibernate.validator.constraints.UUID(message = "not a valid id") String> folderIds,
        @NotNull(message = "can't accept null for boolean")
        Boolean allFiles
) {}
