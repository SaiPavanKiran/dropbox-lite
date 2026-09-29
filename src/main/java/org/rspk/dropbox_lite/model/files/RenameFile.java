package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotBlank;

public record RenameFile(
        @NotBlank String name
) {
}
