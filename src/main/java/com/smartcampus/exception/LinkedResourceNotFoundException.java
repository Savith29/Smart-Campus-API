package com.smartcampus.exception;

/** Part 5.2 — Thrown when sensor POST has a roomId that does not exist. Maps to 422. */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) { super(message); }
}
