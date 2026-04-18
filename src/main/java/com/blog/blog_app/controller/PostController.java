package com.blog.blog_app.controller;

import com.blog.blog_app.config.AppProperties;
import com.blog.blog_app.entity.Category;
import com.blog.blog_app.entity.Post;
import com.blog.blog_app.exception.ResourceNotFoundException;
import com.blog.blog_app.service.FileStorageService;      // ← NEW import
import com.blog.blog_app.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final AppProperties appProperties;
    private final FileStorageService fileStorageService;    // ← NEW injection

    // ------------------------------------------------------------------ //
    //  GET /api/posts                                                      //
    // ------------------------------------------------------------------ //
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        log.info("GET /api/posts");
        return ResponseEntity.ok(postService.getAllPosts());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/posts/{id}                                                 //
    // ------------------------------------------------------------------ //
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        log.info("GET /api/posts/{}", id);
        return ResponseEntity.ok(postService.getPostById(id));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/posts/search?keyword=...                                   //
    // ------------------------------------------------------------------ //
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(postService.searchPosts(keyword));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/posts/category/{categoryId}                               //
    // ------------------------------------------------------------------ //
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Post>> getPostsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(postService.getPostsByCategory(categoryId));
    }

    // ------------------------------------------------------------------ //
    //  POST /api/posts  — create post WITHOUT image                       //
    // ------------------------------------------------------------------ //
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Post> createPost(
            @Valid @RequestBody Post post,
            @RequestParam("categoryId") Long categoryId) {

        log.info("POST /api/posts (JSON) — title='{}'", post.getTitle());
        attachCategory(post, categoryId);
        Post saved = postService.savePost(post, null, appProperties.getUploadDir());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/posts/with-image  — create post WITH image upload        //
    //  ← This is the new dedicated endpoint for multipart uploads          //
    // ------------------------------------------------------------------ //
    @PostMapping(value = "/with-image", consumes = "multipart/form-data")  // ← NEW
    public ResponseEntity<Post> createPostWithImage(
            @Valid @ModelAttribute Post post,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        log.info("POST /api/posts/with-image — title='{}'", post.getTitle());

        attachCategory(post, categoryId);

        // ← FileStorageService handles all validation + disk I/O
        if (image != null && !image.isEmpty()) {
            String storedPath = fileStorageService.store(image);   // ← NEW
            post.setImagePath(storedPath);
            log.info("Image attached to post — path='{}'", storedPath);
        }

        Post saved = postService.savePost(post, null, appProperties.getUploadDir());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ------------------------------------------------------------------ //
    //  POST /api/posts/{id}/image  — upload/replace image on existing post //
    //  ← Dedicated endpoint for updating just the image of a post         //
    // ------------------------------------------------------------------ //
    @PostMapping("/{id}/image")                                            // ← NEW
    public ResponseEntity<Post> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        log.info("POST /api/posts/{}/image — uploading image", id);

        Post post = postService.getPostById(id);   // throws 404 if not found

        // Delete old image from disk before replacing it
        if (post.getImagePath() != null) {
            fileStorageService.delete(post.getImagePath());            // ← NEW
            log.info("Replaced old image for post id={}", id);
        }

        String storedPath = fileStorageService.store(image);           // ← NEW
        post.setImagePath(storedPath);

        Post updated = postService.savePost(post, null, appProperties.getUploadDir());
        return ResponseEntity.ok(updated);
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/posts/{id}                                              //
    // ------------------------------------------------------------------ //
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        log.info("DELETE /api/posts/{}", id);

        // ← Also clean up the image file from disk on post deletion
        Post post = postService.getPostById(id);
        if (post.getImagePath() != null) {
            fileStorageService.delete(post.getImagePath());            // ← NEW
        }

        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS                                                     //
    // ------------------------------------------------------------------ //
    private void attachCategory(Post post, Long categoryId) {
        Category category = postService.getAllCategories()
                .stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        post.setCategory(category);
    }
}