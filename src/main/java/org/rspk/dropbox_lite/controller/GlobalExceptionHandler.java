package org.rspk.dropbox_lite.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.rspk.dropbox_lite.utils.exceptions.ApiException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ApiException.class})
    ResponseEntity<?> apiExceptionHandler(ApiException ex) {
        LinkedHashMap<String,Object> error = new LinkedHashMap<>();
        error.put("error", ex.getMessage());
        error.put("status",ex.getStatus());
        error.put("code",ex.getCode());
        error.put("result",false);
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(value =
            {
                    NoHandlerFoundException.class,
                    HttpMediaTypeNotSupportedException.class,
                    HttpRequestMethodNotSupportedException.class,
                    MissingRequestHeaderException.class,
                    MissingServletRequestParameterException.class
            }
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void negligibleExceptionHandler(Exception ex) {
        logger.warn("ignored client error",ex);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handlerMethodValidationExceptionHandler(HandlerMethodValidationException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getParameterValidationResults().forEach(result -> {

            result.getResolvableErrors().forEach(error -> {

                String message = error.getDefaultMessage();
                String field = "unknown" ;
                if (error instanceof FieldError fieldError) { field = fieldError.getField(); }

                errors.put(field,message != null ? message : "unknown issue with the field");
            });
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 400);
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(value = {Exception.class})
    ResponseEntity<Map<String,Object>> unHandledExceptionHandler(Exception ex){
        LinkedHashMap<String,Object> error = new LinkedHashMap<>();
        error.put("error","error occurred while processing the api request");
        error.put("status",HttpStatus.INTERNAL_SERVER_ERROR);
        error.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("result",false);

        logger.error("an unhandled error occurred while processing an api request: ",ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
