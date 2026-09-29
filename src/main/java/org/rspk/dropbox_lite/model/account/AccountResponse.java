package org.rspk.dropbox_lite.model.account;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.rspk.dropbox_lite.model.common.TemporalRes;

public record AccountResponse(
    String email,
    @JsonUnwrapped
    TemporalRes timestamps
) {}