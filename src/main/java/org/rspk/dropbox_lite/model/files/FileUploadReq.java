package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.rspk.dropbox_lite.utils.annotations.ValidMimeType;

import java.util.UUID;

public record FileUploadReq (
    @org.hibernate.validator.constraints.UUID(message = "not a valid id")
    String folderId,
    @NotBlank(message = "file name must be given")
    String name,
    @ValidMimeType(message = "invalid content type")
    String contentType,
    @NotNull(message = "file size must be given")
    @Positive(message = "size excepts positive value")
    Long size
) {}
