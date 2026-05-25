package com.articlemanager.backend.Exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException{
    private final String resourceType;
    private final Object identifier;

    public ResourceNotFoundException(String resourceType, Object identifier){
        super(resourceType + " not found: "+ identifier);
        this.resourceType = resourceType;
        this.identifier = identifier;
    }
}
