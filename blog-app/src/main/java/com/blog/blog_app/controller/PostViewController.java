package com.blog.blog_app.controller;

import com.blog.blog_app.entity.Category;
import com.blog.blog_app.entity.Post;
import com.blog.blog_app.exception.ResourceNotFoundException;
import com.blog.blog_app.service.FileStorageService;
import com.blog.blog_app.service.PostService;
import com.blog.blog_app.config.AppProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller                     // Returns view names, not JSON
@RequestMapping("/posts")       // Web UI lives at /posts — separate from /api/posts REST endpoints
@RequiredArgsConstructor
@Slf4j
public class PostViewController {

    private final PostService postService;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    // ------------------------------------------------------------------ //
    //  GET /posts — list all posts                                         //
    // ------------------------------------------------------------------ //
    @GetMapping
    public String listPosts(Model model) {
        log.debug("Rendering post list page");
        List<Post> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);
        return "posts/list";    // → templates/posts/list.html
    }

    // ------------------------------------------------------------------ //
    //  GET /posts/{id} — view a single post                               //
    // ------------------------------------------------------------------ //
    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);    // throws 404 via GlobalExceptionHandler
        model.addAttribute("post", post);
        return "posts/view";    // → templates/posts/view.html
    }

    // ------------------------------------------------------------------ //
    //  GET /posts/new — show empty create form                            //
    // ------------------------------------------------------------------ //
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("post", new Post());                     // empty binding object
        model.addAttribute("categories", postService.getAllCategories());
        return "posts/form";    // → templates/posts/form.html
    }

    // ------------------------------------------------------------------ //
    //  POST /posts/new — handle form submission                           //
    // ------------------------------------------------------------------ //
    @PostMapping("/new")
    public String createPost(
            @Valid @ModelAttribute("post") Post post,               // binds + validates form fields
            BindingResult bindingResult,                            // holds validation errors
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model,
            RedirectAttributes redirectAttributes) {

        // If validation failed — re-render form WITH errors, don't redirect
        if (bindingResult.hasErrors()) {
            log.warn("Post form validation failed: {}", bindingResult.getAllErrors());
            model.addAttribute("categories", postService.getAllCategories());
            return "posts/form";    // stay on form — Thymeleaf shows field errors
        }

        // Attach the chosen Category
        Category category = postService.getAllCategories()
                .stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        post.setCategory(category);

        // Store image if provided
        if (image != null && !image.isEmpty()) {
            String storedPath = fileStorageService.store(image);
            post.setImagePath(storedPath);
        }



        postService.savePost(post, null, appProperties.getUploadDir());

        // PRG pattern — redirect after POST prevents duplicate submission on refresh
        redirectAttributes.addFlashAttribute("successMessage", "Post created successfully!");
        return "redirect:/posts";
    }

    // ------------------------------------------------------------------ //
    //  POST /posts/{id}/delete — delete a post                            //
    // ------------------------------------------------------------------ //
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Post post = postService.getPostById(id);

        if (post.getImagePath() != null) {
            fileStorageService.delete(post.getImagePath());
        }

        postService.deletePost(id);
        log.info("Post id={} deleted via web UI", id);

        redirectAttributes.addFlashAttribute("successMessage", "Post deleted.");
        return "redirect:/posts";
    }
}