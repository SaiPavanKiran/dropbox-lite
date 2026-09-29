package org.rspk.dropbox_lite.model.account;

import java.util.UUID;

public record AuthenticatedUser (
        UUID accountId,
        String username
)  {}
