package com.articlemanager.backend.DTOs.Response;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFollowerResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Instant followedAt;
}
