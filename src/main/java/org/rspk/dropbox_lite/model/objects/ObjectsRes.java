package org.rspk.dropbox_lite.model.objects;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.rspk.dropbox_lite.model.common.TemporalRes;
import org.rspk.dropbox_lite.model.files.UploadStatus;

import java.util.UUID;

public sealed interface ObjectsRes permits ObjectsRes.FolderObj ,ObjectsRes.FileObj {

    record FolderObj(
            ObjectType type,
            UUID folderId,
            String name,
            UUID parentFolderId,
            @JsonUnwrapped
            TemporalRes timestamps
    ) implements ObjectsRes {}

    record FileObj(
            ObjectType type,
            UUID folderId,
            String name,
            String contentType,
            Long size,
            Boolean archived,
            UploadStatus uploadStatus,
            @JsonUnwrapped
            TemporalRes timestamps
    ) implements ObjectsRes {}

}
