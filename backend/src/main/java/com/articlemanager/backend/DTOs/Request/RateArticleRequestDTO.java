package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateArticleRequestDTO {

    @NotNull(message = "user id is required")
    private Long userId;

    @NotNull(message = "article id is required")
    private Integer articleId;

    @NotNull(message = "rating is required")
    private Short rating;

}