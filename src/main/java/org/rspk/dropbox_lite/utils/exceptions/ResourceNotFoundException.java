package org.rspk.dropbox_lite.utils.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException{
    public ResourceNotFoundException(String error) {
        super(HttpStatus.NOT_FOUND,error);
    }
}


