package com.blog.blog_app.repository;

import com.blog.blog_app.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Spring Data JPA auto-implements this from the method name alone
    Optional<Category> findByName(String name);
}
