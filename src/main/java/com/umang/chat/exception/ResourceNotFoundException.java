package com.umang.chat.exception;

/** Thrown when a referenced user, conversation, or message does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
