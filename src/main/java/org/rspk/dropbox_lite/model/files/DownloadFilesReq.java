package org.rspk.dropbox_lite.model.files;

import java.util.List;
import java.util.UUID;

public record DownloadFilesReq(
        List<UUID> fileIds
) {}
