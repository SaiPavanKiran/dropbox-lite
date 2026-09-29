package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status,String error) {
        super(error);
        this.status = status;
    }


    public int getCode() {
        return status.value();
    }

    public HttpStatus getStatus() {
        return status;
    }
}
