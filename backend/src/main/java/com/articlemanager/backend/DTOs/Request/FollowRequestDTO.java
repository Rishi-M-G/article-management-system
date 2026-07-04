package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowRequestDTO {
    @NotNull(message = "followerId cannot be null")
    private Long followerId;

    @NotNull(message = "followingId cannot be null")
    private Long followingId;
}
