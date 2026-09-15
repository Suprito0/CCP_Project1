package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranscriptionController {

    @PostMapping("/transcribe")
    public String transcribe(
            @RequestParam("audio") MultipartFile audioFile) {

        System.out.println(
                "Received audio: " + audioFile.getSize() + " bytes"
        );

        return "Audio received";
    }
}