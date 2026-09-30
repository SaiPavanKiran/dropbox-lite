package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotBlank;

public record RenameFile(
        @NotBlank(message = "name can't be blank") String name
) {
}
