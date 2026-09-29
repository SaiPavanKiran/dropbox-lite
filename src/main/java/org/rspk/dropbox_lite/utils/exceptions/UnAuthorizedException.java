package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends ApiException{
    public UnAuthorizedException(String error) {
        super(HttpStatus.UNAUTHORIZED,error);
    }
}
