package com.example.SpeechToText.service;

import org.springframework.web.multipart.MultipartFile;

//Utility methods for validating and normalising uploaded audio files.
final class AudioFileSupport {

    // Prevent accidental construction.
    private AudioFileSupport() {
    }

    // Reject invalid uploads before any cloud request is attempted.
    static void validate(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file is empty");
        }

        // OpenAI's transcription endpoint accepts files up to 25 MB.
        if (audioFile.getSize() > 25L * 1024L * 1024L) {
            throw new IllegalArgumentException("Audio file is too large");
        }
    }

    static String normaliseMimeType(MultipartFile audioFile) {
        String mimeType = audioFile.getContentType();

        if (mimeType != null && !mimeType.isBlank()
                && !mimeType.equalsIgnoreCase("application/octet-stream")) {
            return mimeType.split(";")[0].trim().toLowerCase();
        }

        String filename = audioFile.getOriginalFilename();
        if (filename != null) {

            String lower = filename.toLowerCase();

            if (lower.endsWith(".ogg")) {
                return "audio/ogg";
            }
            if (lower.endsWith(".wav")) {
                return "audio/wav";
            }
            if (lower.endsWith(".mp3")) {
                return "audio/mpeg";
            }
            if (lower.endsWith(".m4a")) {
                return "audio/mp4";
            }
        }

        return "audio/webm";
    }

    static String safeFilename(MultipartFile audioFile, String mimeType) {
        String original = audioFile.getOriginalFilename();
        if (original != null && !original.isBlank()) {
            return original.replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return switch (mimeType) {
            case "audio/ogg" -> "recording.ogg";
            case "audio/wav" -> "recording.wav";
            case "audio/mpeg" -> "recording.mp3";
            case "audio/mp4" -> "recording.m4a";
            default -> "recording.webm";
        };
    }
}
