package com.blog.blog_app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "blog")  // Binds all "blog.*" keys from properties files
@Validated                                  // Enforces @NotBlank at startup — fails fast on misconfiguration
@Getter
@Setter
public class AppProperties {

    @NotBlank(message = "blog.upload-dir must be configured")
    private String uploadDir;   // Maps to: blog.upload-dir in properties files

    private int maxFileSizeMb = 5;          // Maps to: blog.max-file-size-mb (default: 5)
    private String allowedFileTypes = "jpg,jpeg,png,gif"; // Maps to: blog.allowed-file-types
}