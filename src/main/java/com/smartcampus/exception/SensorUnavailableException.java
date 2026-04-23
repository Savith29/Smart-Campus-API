package com.smartcampus.exception;

/** Part 5.3 — Thrown when POSTing a reading to a MAINTENANCE sensor. Maps to 403. */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) { super(message); }
}
