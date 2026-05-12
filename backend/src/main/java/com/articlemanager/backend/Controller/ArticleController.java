package com.articlemanager.backend.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.ArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ArticleResponseDTO;
import com.articlemanager.backend.Service.ArticleService;
import com.articlemanager.backend.entity.Articles;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "Get All Articles")
    @GetMapping("/all")
    public ResponseEntity<List<Articles>> getAllArticles() {
        List<Articles> articles = articleService.getAllArticles();
        return ResponseEntity.status(HttpStatus.OK).body(articles);
    }

    @Operation(summary = "Get article by ID")
    @GetMapping
    public ResponseEntity<ArticleResponseDTO> getArticleById(@RequestParam Integer articleId) {
        ArticleResponseDTO responseDTO = articleService.getArticleById(articleId);
        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @Operation(summary = "Add Article")
    @PostMapping("/add")
    public ResponseEntity<ArticleResponseDTO> addArticle(@Valid @RequestBody ArticleRequestDTO requestDTO) {
        ArticleResponseDTO responseDTO = articleService.addArticle(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
}
