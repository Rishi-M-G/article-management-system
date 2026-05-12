package com.articlemanager.backend.DTOs.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * What will i need as payload while registering
 * 1. Email
 * 2. First Name
 * 3. Last Name
 * 4. Password
 */
@Getter
@Setter
public class RegisterRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be atleast 8 characters")
    String password;

    @NotBlank(message = "First name is required")
    String firstName;

    @NotBlank(message = "Last name is required")
    String lastName;
}
