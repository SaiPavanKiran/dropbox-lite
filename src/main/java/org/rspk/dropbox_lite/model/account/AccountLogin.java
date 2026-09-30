package org.rspk.dropbox_lite.model.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountLogin(
        @NotBlank(message = "email can't be blank")
        @Email(message = "not a valid email")
        @Size( min = 6, max = 254, message = "Email must be between 6 and 254 characters")
        String email,
        @NotNull(message = "password can't be null")
        @Size(min = 8, max = 30,
                message = "password must be between 8 and 30 characters")
        String password
) {
}
