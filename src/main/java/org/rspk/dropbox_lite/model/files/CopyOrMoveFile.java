package org.rspk.dropbox_lite.model.files;


import java.util.UUID;

public record CopyOrMoveFile (
        UUID toFolder
){
}
