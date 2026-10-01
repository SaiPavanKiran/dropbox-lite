package org.rspk.dropbox_lite.model.files;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.rspk.dropbox_lite.model.common.TemporalRes;

public record DownloadFileRes (
        String name,
        String contentType,
        Long size,
        String url,
        @JsonUnwrapped
        TemporalRes temporalRes
){}
