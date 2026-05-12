package com.articlemanager.backend.Controller;

import java.util.List;

import org.apache.catalina.connector.Response;
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
import com.articlemanager.backend.DTOs.Response.LoginResponseDTO;
import com.articlemanager.backend.Service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        userService.registerUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        userService.loginUser(requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body("Login successfull");
    }

    @Operation(summary = "Fetch all profiles")
    @GetMapping("/allprofiles")
    public ResponseEntity<List<LoginResponseDTO>> allProfiles() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getAllProfiles());
    }

    @Operation(summary = "Fetch Profile by ID")
    @GetMapping("/profile")
    public ResponseEntity<LoginResponseDTO> getProfileById(@RequestParam Long UserId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getProfileById(UserId));
    }

    @Operation(summary = "Update Profile")
    @PutMapping("/updateProfile")
    public ResponseEntity<LoginResponseDTO> updateProfile(@RequestParam Long UserID,
            @Valid @RequestBody UserUpdateRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateProfile(UserID, requestDTO));
    }
}
