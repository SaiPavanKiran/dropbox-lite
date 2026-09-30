package org.rspk.dropbox_lite.model.files;


import jakarta.validation.constraints.NotBlank;

public record CopyFile(
        @NotBlank(message = "to folder must not be blank")
        @org.hibernate.validator.constraints.UUID(message = "not a valid id")
        String toFolder
){}
