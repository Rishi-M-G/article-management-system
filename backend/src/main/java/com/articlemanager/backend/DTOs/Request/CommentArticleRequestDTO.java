package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentArticleRequestDTO {

    @NotNull(message = "user id is required")
    private Long userId;

    @NotNull(message = "article id is required")
    private Long articleId;

    @NotNull(message = "comment is required")
    @Size(min = 1, max = 500, message = "comment must be between 1 and 500 characters")
    private String comment;
    
}
