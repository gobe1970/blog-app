package com.blog.blog_app.service;

import com.blog.blog_app.entity.Category;
import com.blog.blog_app.entity.Post;
import com.blog.blog_app.exception.ResourceNotFoundException;   // ← THIS import was missing
import com.blog.blog_app.repository.CategoryRepository;
import com.blog.blog_app.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    // ------------------------------------------------------------------ //
    //  READ                                                                //
    // ------------------------------------------------------------------ //

    public List<Post> getAllPosts() {
        log.debug("Fetching all posts with their categories");
        return postRepository.findAllWithCategory();
    }

    public Post getPostById(Long id) {
        log.debug("Fetching post with id={}", id);
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));  // ✅ typed exception
    }

    public List<Post> getPostsByCategory(Long categoryId) {
        return postRepository.findByCategoryId(categoryId);
    }

    public List<Post> searchPosts(String keyword) {
        log.debug("Searching posts with keyword='{}'", keyword);
        return postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, keyword);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ------------------------------------------------------------------ //
    //  WRITE                                                               //
    // ------------------------------------------------------------------ //

    // PostService.java — savePost() must NOT overwrite imagePath when imageFile is null
    @Transactional
    public Post savePost(Post post, MultipartFile imageFile, String uploadDir) {

        // imageFile will be null when called from PostViewController
        // because the controller already called fileStorageService.store()
        // DO NOT reset post.setImagePath() here unconditionally
        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = storeFile(imageFile, uploadDir);
            post.setImagePath(imagePath);
        }

        // imagePath set by the controller is preserved here
        Post saved = postRepository.save(post);
        log.info("Post saved id={}, imagePath='{}'", saved.getId(), saved.getImagePath());
        return saved;
    }

    @Transactional
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post", "id", id);  // ✅ typed exception
        }
        postRepository.deleteById(id);
        log.info("Post deleted with id={}", id);
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS                                                     //
    // ------------------------------------------------------------------ //

    private String storeFile(MultipartFile file, String uploadDir) {
        try {
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.debug("Created upload directory at '{}'", uploadPath.toAbsolutePath());
            }

            String originalFilename = file.getOriginalFilename();
            String safeFilename = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadPath.resolve(safeFilename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return uploadDir + "/" + safeFilename;

        } catch (IOException e) {
            log.error("Failed to store file '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Could not store file. Please try again.", e);
        }
    }
}