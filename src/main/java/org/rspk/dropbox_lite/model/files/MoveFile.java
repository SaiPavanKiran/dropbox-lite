package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MoveFile (
        @NotBlank(message = "to folder must not be blank")
        @org.hibernate.validator.constraints.UUID(message = "not a valid id")
        String toFolder,
        @NotNull(message = "replace only allows boolean value")
        boolean replace
){}
