package com.articlemanager.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import com.articlemanager.backend.DTOs.Request.LoginRequestDTO;
import com.articlemanager.backend.DTOs.Request.RegisterRequestDTO;
import com.articlemanager.backend.DTOs.Request.UserUpdateRequestDTO;
import com.articlemanager.backend.DTOs.Response.LoginResponseDTO;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.entity.User;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public User registerUser(RegisterRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setEmail(requestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setFirstName(requestDTO.getFirstName());
        user.setLastName(requestDTO.getLastName());

        return userRepository.save(user);
    }

    public LoginResponseDTO loginUser(LoginRequestDTO requestDTO) {

        User user = userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("No user registered with this email address"));

        // Password check
        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setEmail(user.getEmail());
        responseDTO.setFirstName(user.getFirstName());
        responseDTO.setLastName(user.getLastName());

        return responseDTO;
    }

    public List<LoginResponseDTO> getAllProfiles() {
        List<User> users = userRepository.findAll();
        log.info("🔥🔥🔥 All users are fetched");

        List<LoginResponseDTO> allUsers = new ArrayList<>();

        for (User user : users) {
            LoginResponseDTO responseDTO = new LoginResponseDTO();
            responseDTO.setId(user.getId());
            responseDTO.setFirstName(user.getFirstName());
            responseDTO.setLastName(user.getLastName());
            responseDTO.setEmail(user.getEmail());

            allUsers.add(responseDTO);
        }

        return allUsers;
    }

    public LoginResponseDTO getProfileById(Long Id) {
        User optionalUser = userRepository.findById(Id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setId(optionalUser.getId());
        responseDTO.setFirstName(optionalUser.getFirstName());
        responseDTO.setLastName(optionalUser.getLastName());
        responseDTO.setEmail(optionalUser.getEmail());

        return responseDTO;
    }

    @Transactional
    public LoginResponseDTO updateProfile(Long UserId, UserUpdateRequestDTO requestDTO) {

        User optionalUser = userRepository.findById(UserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        optionalUser.setEmail(requestDTO.getEmail());
        optionalUser.setFirstName(requestDTO.getFirstName());
        optionalUser.setLastName(requestDTO.getLastName());

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setEmail(optionalUser.getEmail());
        responseDTO.setFirstName(optionalUser.getFirstName());
        responseDTO.setLastName(optionalUser.getLastName());
        responseDTO.setId(optionalUser.getId());
        return responseDTO;
    }
}
