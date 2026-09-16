package com.example.SpeechToText.service;

import org.springframework.web.multipart.MultipartFile;

// Speech-to-text contract used by the HTTP controller. 
public interface SpeechToTextService {

    TranscriptionResult transcribe(MultipartFile audioFile);
}
