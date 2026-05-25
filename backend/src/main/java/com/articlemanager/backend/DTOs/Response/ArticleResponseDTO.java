package com.articlemanager.backend.DTOs.Response;

import java.time.Instant;

import com.articlemanager.backend.entity.ArticleStatus;
import com.articlemanager.backend.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleResponseDTO {
    private Long id;

    private String heading;

    private String content;

    private String summary;

    private String slug;

    private String authorName;

    private ArticleStatus status;

    private Instant createdAt;

    private Instant updatedAt;

}
