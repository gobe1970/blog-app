package com.blog.blog_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
@Profile("dev")     // This entire class is only loaded when spring.profiles.active=dev
@Slf4j
public class DevConfig {

    /**
     * Logs every incoming HTTP request in dev — method, URI, headers, payload.
     * Never enable this in prod: it logs request bodies which may contain passwords.
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        log.info("DEV profile active — HTTP request logging enabled");

        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1000);
        filter.setIncludeHeaders(false);    // headers often contain tokens — skip even in dev
        filter.setAfterMessagePrefix("REQUEST: ");
        return filter;
    }
}