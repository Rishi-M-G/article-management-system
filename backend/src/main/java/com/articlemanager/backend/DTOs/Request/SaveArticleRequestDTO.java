package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveArticleRequestDTO {
    @NotNull(message = "userId cannot be null")
    Long userId;
}
