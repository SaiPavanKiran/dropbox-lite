package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class SomethingWentWrongException extends ApiException {
    public SomethingWentWrongException(String error) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }
}
