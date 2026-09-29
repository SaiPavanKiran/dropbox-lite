package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String error) {
        super(HttpStatus.BAD_REQUEST,error);
    }
}
