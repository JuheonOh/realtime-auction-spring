package com.inhatc.auction.domain.lifecycle.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class FileUtils {
    private static final String SHUTDOWN_TIME_FILE = "shutdown_time.txt";

    public void writeShutdownTime(LocalDateTime time) {
        try {
            Files.writeString(Path.of(SHUTDOWN_TIME_FILE), time.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("파일에 서버 종료 시간 쓰기 실패", e);
        }
    }

    public LocalDateTime readLastShutdownTime() {
        try {
            String timeStr = Files.readString(Path.of(SHUTDOWN_TIME_FILE), StandardCharsets.UTF_8);
            return LocalDateTime.parse(timeStr);
        } catch (IOException e) {
            log.error("파일에서 서버 종료 시간 읽기 실패", e);
            return null;
        }
    }
}
