package org.rspk.dropbox_lite.model.share;

import jakarta.validation.constraints.Email;
import org.hibernate.validator.constraints.Range;

import java.util.UUID;

public record ShareObjectReq (
        UUID objectId,
        @Email
        String recipientEmail,
        @Range(min = 30,message = "min share request is 15 min")
        Long shareDurationInMin
){}
