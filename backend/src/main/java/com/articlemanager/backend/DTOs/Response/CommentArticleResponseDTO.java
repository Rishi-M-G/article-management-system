package com.articlemanager.backend.DTOs.Response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentArticleResponseDTO {
    
    private Long id;

    private String comment;

    private Integer totalComments;
}
