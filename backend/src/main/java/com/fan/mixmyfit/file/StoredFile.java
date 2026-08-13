package com.fan.mixmyfit.file;

public record StoredFile(
        String path,
        String originalFilename,
        String contentType,
        long fileSize) {
}
