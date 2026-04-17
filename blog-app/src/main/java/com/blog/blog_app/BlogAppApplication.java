package com.blog.blog_app;

import com.blog.blog_app.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class) // Registers the config bean
public class BlogAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(BlogAppApplication.class, args);
	}
}