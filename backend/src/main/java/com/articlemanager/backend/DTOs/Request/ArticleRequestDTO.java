package com.articlemanager.backend.DTOs.Request;

import com.articlemanager.backend.entity.ArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleRequestDTO {

    @NotBlank(message = "Title is required")
    private String heading;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Author id is required")
    private Long authorId;
}
