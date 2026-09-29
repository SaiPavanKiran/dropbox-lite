package org.rspk.dropbox_lite.model.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountReq(
        @NotNull(message = "email can't be null")
        @Email(message = "not a valid email")
        String email,
        @NotNull(message = "password can't be null")
        @Size(min = 8, max = 30,
                message = "password must be between 8 and 30 characters")
        String password,
        @NotNull @NotBlank @Size(min = 8,max = 8,message = "otp must be 8 chars") String otp
) {}

