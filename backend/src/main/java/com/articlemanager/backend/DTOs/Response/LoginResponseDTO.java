package com.articlemanager.backend.DTOs.Response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDTO {
    Long Id;

    String email;

    String firstName;

    String lastName;
}
