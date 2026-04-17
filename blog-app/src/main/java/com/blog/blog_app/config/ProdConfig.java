package com.blog.blog_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("prod")    // Only loaded when spring.profiles.active=prod
@Slf4j
public class ProdConfig implements WebMvcConfigurer {

    /**
     * In prod, uploaded images live outside the JAR at /var/app/uploads.
     * This resource handler maps GET /uploads/** to that external directory
     * so Thymeleaf <img th:src="@{'/uploads/' + ${post.imagePath}}"> works correctly.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("PROD profile active — mapping /uploads/** to external filesystem");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/var/app/uploads/");
    }
}