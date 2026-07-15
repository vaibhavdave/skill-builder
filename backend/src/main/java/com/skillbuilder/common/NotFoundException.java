package com.skillbuilder.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource, Long id) {
        return new NotFoundException(resource + " " + id + " not found");
    }
}
