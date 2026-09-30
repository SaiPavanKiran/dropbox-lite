package org.rspk.dropbox_lite.model.share;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Range;

import java.util.UUID;

public record ShareObjectReq (
        @NotBlank
        @org.hibernate.validator.constraints.UUID(message = "not a valid id")
        String objectId,
        @NotBlank(message = "email can't be blank")
        @Email(message = "invalid email")
        String recipientEmail,
        @NotNull
        @Min(value = 30, message = "min share duration is 30 min")
        long shareDurationInMin
){}
