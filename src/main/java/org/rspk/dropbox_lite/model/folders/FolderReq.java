package org.rspk.dropbox_lite.model.folders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FolderReq (
        @org.hibernate.validator.constraints.UUID(message = "not a valid id")
        String parentFolderId,
        @NotBlank(message = "folder name must be given")
        @Size(max = 255, message = "folder name can't exceeded 255 chars")
        String name
) {}
