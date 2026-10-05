package com.umang.chat.exception;

/** Thrown for semantically invalid requests (e.g. a one-to-one conversation without 2 members). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
