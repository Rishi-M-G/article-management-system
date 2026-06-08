package com.articlemanager.backend.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.ArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ApiResponse;
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleResponseDTO>>> getAllArticles() {
        List<Articles> articles = articleService.getAllArticles();

        List<ArticleResponseDTO> responseDTOs = new ArrayList<>();

        for (Articles articles2 : articles) {
            ArticleResponseDTO responseDTO = new ArticleResponseDTO();
            responseDTO.setId(articles2.getId());
            responseDTO.setHeading(articles2.getHeading());
            responseDTO.setContent(articles2.getContent());
            responseDTO.setSummary(articles2.getSummary());
            responseDTO.setSlug(articles2.getSlug());
            responseDTO.setAuthorName(articles2.getAuthor().getFirstName());
            responseDTO.setStatus(articles2.getStatus());
            responseDTO.setCreatedAt(articles2.getCreatedAt());
            responseDTO.setUpdatedAt(articles2.getUpdatedAt());

            responseDTOs.add(responseDTO);

        }
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("All articles fetched");
        apiResponse.setData(apiResponse);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Get article by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDTO> getArticleById(@PathVariable Long articleId) {
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
