package org.rspk.dropbox_lite.model.files;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record DownloadFilesReq(
        @NotEmpty(message = "file ids must be given")
        List<@org.hibernate.validator.constraints.UUID(message = "not a valid id") String> fileIds
) {}

