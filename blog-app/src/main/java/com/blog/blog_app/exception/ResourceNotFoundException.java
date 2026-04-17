package com.blog.blog_app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)   // Binds this exception to HTTP 404 by default
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;  // e.g. "Post"
    private final String fieldName;     // e.g. "id"
    private final Object fieldValue;    // e.g. 42

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Produces: "Post not found with id: 42"
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    // Getters — no Lombok here intentionally so the class is self-contained
    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}