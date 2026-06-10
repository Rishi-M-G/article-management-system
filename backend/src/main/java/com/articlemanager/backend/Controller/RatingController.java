package com.articlemanager.backend.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.RateArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ApiResponse;
import com.articlemanager.backend.DTOs.Response.RateArticleResponseDTO;
import com.articlemanager.backend.Service.RatingService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/rate")
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "Rate an article")
    @PostMapping
    public ResponseEntity<ApiResponse<RateArticleResponseDTO>> rateArticle(
            @Valid @RequestBody RateArticleRequestDTO requestDTO) {
        RateArticleResponseDTO responseDTO = ratingService.rateArticle(requestDTO);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Article rated successfully");
        apiResponse.setData(responseDTO);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);

    }
}
