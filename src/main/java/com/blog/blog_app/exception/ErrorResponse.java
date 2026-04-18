package com.blog.blog_app.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error payload returned for ALL exceptions.
 * Every error the API produces looks the same — clients can rely on this contract.
 *
 * Example JSON:
 * {
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "Post not found with id: '99'",
 *   "path":      "/api/posts/99",
 *   "timestamp": "2025-06-10 14:32:01",
 *   "validationErrors": null
 * }
 */
@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // Only populated for validation failures (422), null for everything else
    private Map<String, String> validationErrors;
}