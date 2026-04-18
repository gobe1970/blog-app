package com.blog.blog_app.repository;

import com.blog.blog_app.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Derived query — Spring Data parses the method name and builds the JPQL
    List<Post> findByCategoryId(Long categoryId);

    // Explicit JPQL with JOIN FETCH to avoid N+1 problem when loading posts + category together
    @Query("SELECT p FROM Post p JOIN FETCH p.category ORDER BY p.id DESC")
    List<Post> findAllWithCategory();

    // Derived query for keyword search across title and content
    List<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content);
}