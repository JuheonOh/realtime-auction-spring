package com.inhatc.auction.global.utils;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class TimeUtils {
    public static String getRelativeTimeString(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        Long diffTime = Duration.between(createdAt, now).toSeconds();
        Long minutes = Math.floorDiv(diffTime, 60);
        Long hours = Math.floorDiv(minutes, 60);
        Long days = Math.floorDiv(hours, 24);

        String time;
        if (diffTime < 60) {
            time = "방금 전";
        } else if (minutes < 60) {
            time = String.format("%d분 전", minutes);
        } else if (hours < 24) {
            time = String.format("%d시간 전", hours);
        } else {
            time = String.format("%d일 전", days);
        }

        return time;
    }
}
