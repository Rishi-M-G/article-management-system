package com.articlemanager.backend.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.SaveArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ApiResponse;
import com.articlemanager.backend.DTOs.Response.ArticleResponseDTO;
import com.articlemanager.backend.Service.SavedArticleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class SavedArticleController {

    private final SavedArticleService savedArticleService;

    @PutMapping("/save")
    public ResponseEntity<ApiResponse<Void>> saveArticle(@Valid @RequestBody SaveArticleRequestDTO requestDTO,
            @RequestParam Long id) {
        Boolean isSavedAlready = savedArticleService.saveArticle(id, requestDTO.getUserId());
        ApiResponse apiResponse = new ApiResponse<>();
        if (isSavedAlready) {
            apiResponse.setMessage("Article Saved");
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } else {
            apiResponse.setMessage("Article saved already");
            return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
        }
    }

    @DeleteMapping("/save")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@RequestParam Long id, @RequestParam Long userId) {
        savedArticleService.unSaveArticle(id, userId);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Article unsaved successfully");
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @GetMapping("/saved-articles")
    public ResponseEntity<ApiResponse<List<ArticleResponseDTO>>> getSavedArticles(@RequestParam Long userId) {
        List<ArticleResponseDTO> responseDTOs = savedArticleService.getSavedArticles(userId);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setData(responseDTOs);
        apiResponse.setMessage("Saved articles retrieved");
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

}
