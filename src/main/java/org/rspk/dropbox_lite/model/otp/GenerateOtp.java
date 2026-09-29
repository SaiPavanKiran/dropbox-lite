package org.rspk.dropbox_lite.model.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record GenerateOtp (
        @NotNull(message = "email can't be null")
        @Email(message = "not a valid email")
        String email
) {}
