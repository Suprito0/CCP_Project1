package com.example.SpeechToText.controller;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.SpeechToText.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

//Produces the exact YAML error shape for unexpected operational API failures.
@RestControllerAdvice(assignableTypes = SystemController.class)
public class ApiExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnexpected(
                        Exception exception,
                        HttpServletRequest request) {

                // Log only the path and exception type
                log.error("Operational API failure path={} type={}",
                                request.getRequestURI(),
                                exception.getClass().getSimpleName());

                ErrorResponse body = new ErrorResponse(
                                Instant.now(),
                                500,
                                "Internal Server Error",
                                "An unexpected server error occurred.",
                                request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(body);
        }
}
