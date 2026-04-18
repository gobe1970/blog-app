package com.blog.blog_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Reads the active upload dir from whichever profile is loaded
    @Value("${blog.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Dev mapping — serves uploaded files from the local relative path.
     * ProdConfig overrides this with an absolute filesystem path.
     * Both map the same URL pattern /uploads/** so templates need no changes.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}