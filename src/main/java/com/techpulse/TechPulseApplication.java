package com.techpulse;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Main entry point for TECHPULSE — Discover. Participate. Achieve.
 * Course Code: 2113611 (Full Stack Java Programming)
 */
@SpringBootApplication
public class TechPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechPulseApplication.class, args);
    }

    @PostConstruct
    public void initDefaultTimeZone() {
        // Enforce Asia/Kolkata (IST) as the default JVM timezone for consistent calculations
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }
}
