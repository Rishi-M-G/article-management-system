package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateArticleRequestDTO {

    @NotNull(message = "user id is required")
    private Long userId;

    @NotNull(message = "article id is required")
    private Long articleId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Short rating;

}