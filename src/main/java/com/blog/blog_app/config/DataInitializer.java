package com.blog.blog_app.config;

import com.blog.blog_app.entity.Category;
import com.blog.blog_app.entity.Post;
import com.blog.blog_app.repository.CategoryRepository;
import com.blog.blog_app.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")         // Seeds data ONLY in dev — never touches prod DB
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final PostRepository     postRepository;

    /**
     * CommandLineRunner runs once after the entire Spring context is loaded.
     * Safe to use with JPA because all repositories are fully ready.
     *
     * We guard with a count check so re-runs (e.g. hot-reload) don't duplicate data.
     */
    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {

            if (categoryRepository.count() > 0) {
                log.info("DataInitializer — data already present, skipping seed.");
                return;
            }

            log.info("DataInitializer — seeding categories and posts...");

            // ── Categories ────────────────────────────────────────────
            Category tech = categoryRepository.save(
                    Category.builder().name("Technology").build());

            Category lifestyle = categoryRepository.save(
                    Category.builder().name("Lifestyle").build());

            Category tutorial = categoryRepository.save(
                    Category.builder().name("Tutorials").build());

            log.info("Seeded {} categories", categoryRepository.count());

            // ── Posts ─────────────────────────────────────────────────
            List<Post> posts = List.of(
                    Post.builder()
                            .title("Getting Started with Spring Boot")
                            .content("""
                                Spring Boot makes it incredibly easy to create
                                stand-alone, production-grade Spring-based applications.
                                It takes an opinionated view of the Spring platform so
                                that new and existing users can quickly get to the bits
                                they need. In this post we explore auto-configuration,
                                embedded servers, and starter dependencies.
                                """)
                            .category(tech)
                            .build(),

                    Post.builder()
                            .title("Understanding JPA and Hibernate")
                            .content("""
                                JPA (Jakarta Persistence API) is the standard ORM
                                specification for Java. Hibernate is the most widely used
                                implementation. Together they let you map Java classes to
                                database tables and write queries in JPQL rather than SQL.
                                We cover entities, relationships, lazy loading, and the
                                N+1 problem in this deep-dive.
                                """)
                            .category(tutorial)
                            .build(),

                    Post.builder()
                            .title("Work-Life Balance for Developers")
                            .content("""
                                Burnout is real and the software industry is not immune.
                                This post shares practical strategies for setting boundaries,
                                taking meaningful breaks, staying physically active, and
                                protecting your deep-work time so you can sustain a long,
                                healthy career without sacrificing the things that matter most.
                                """)
                            .category(lifestyle)
                            .build(),

                    Post.builder()
                            .title("Building REST APIs with Spring MVC")
                            .content("""
                                In this tutorial we build a fully functional REST API
                                using @RestController, ResponseEntity, proper HTTP status
                                codes, global exception handling with @ControllerAdvice,
                                and Bean Validation. We also look at content negotiation
                                and how to version your API endpoints effectively.
                                """)
                            .category(tutorial)
                            .build()
            );

            postRepository.saveAll(posts);
            log.info("DataInitializer — seeded {} posts. Ready!", postRepository.count());
        };
    }
}