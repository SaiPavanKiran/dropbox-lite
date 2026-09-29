package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class DataNotUniqueException extends ApiException {
    public DataNotUniqueException(String error) {
        super(HttpStatus.CONFLICT,error);
    }
}


