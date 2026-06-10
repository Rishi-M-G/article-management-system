package com.articlemanager.backend.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.LoginRequestDTO;
import com.articlemanager.backend.DTOs.Request.RegisterRequestDTO;
import com.articlemanager.backend.DTOs.Request.UserUpdateRequestDTO;
import com.articlemanager.backend.DTOs.Response.ApiResponse;
import com.articlemanager.backend.DTOs.Response.LoginResponseDTO;
import com.articlemanager.backend.Service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        userService.registerUser(requestDTO);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("User registered successfully");
        apiResponse.setData(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO requestDTO) {

        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Login successful");
        apiResponse.setData(userService.loginUser(requestDTO));
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Fetch all profiles")
    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<List<LoginResponseDTO>>> allProfiles() {
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("All profiles fetched successfully");
        apiResponse.setData(userService.getAllProfiles());
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Fetch Profile by ID")
    @GetMapping("/profiles/{id}")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> getProfileById(@PathVariable Long UserId) {
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Profile fetched");
        apiResponse.setData(userService.getProfileById(UserId));
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @Operation(summary = "Update Profile")
    @PutMapping("/profiles/{id}")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> updateProfile(@RequestParam Long UserID,
            @Valid @RequestBody UserUpdateRequestDTO requestDTO) {
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Profile updated");
        apiResponse.setData(userService.updateProfile(UserID, requestDTO));
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }
}
