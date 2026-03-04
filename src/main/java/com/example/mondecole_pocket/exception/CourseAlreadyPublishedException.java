package com.example.mondecole_pocket.exception;

public class CourseAlreadyPublishedException extends RuntimeException {
    public CourseAlreadyPublishedException(String message) {
        super(message);
    }
}