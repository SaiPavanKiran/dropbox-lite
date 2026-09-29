package org.rspk.dropbox_lite.model.folders;

import jakarta.validation.constraints.NotBlank;

public record RenameFolder (
        @NotBlank String name
){
}
