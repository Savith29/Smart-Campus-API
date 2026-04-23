package com.smartcampus.exception;

/** Part 5.1 — Thrown when deleting a room that still has sensors assigned. Maps to 409. */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) { super(message); }
}
