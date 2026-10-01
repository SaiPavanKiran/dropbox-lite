package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FilesArchiveReq(
        @NotEmpty
        List<@org.hibernate.validator.constraints.UUID(message = "not a valid id") String> fileIds
        //needs extra configuration -- pushed it to next version
        /*List<@org.hibernate.validator.constraints.UUID(message = "not a valid id") String> folderIds,
        @NotNull(message = "can't accept null for boolean")
        Boolean allFiles*/
) {}
