package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TranscriptionController {
    @PostMapping("/transcribe")
    public String transcribe() {
        return "Audio received";
    }
} 
