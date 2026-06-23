package com.articlemanager.backend.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.articlemanager.backend.DTOs.Request.CommentArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ApiResponse;
import com.articlemanager.backend.DTOs.Response.CommentArticleResponseDTO;
import com.articlemanager.backend.Service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/comments")
public class CommentsController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentArticleResponseDTO>> commentArticle(
            @Valid @RequestBody CommentArticleRequestDTO requestDTO) {
        CommentArticleResponseDTO responseDTO = commentService.commentArticle(requestDTO);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Comment saved successfully");
        apiResponse.setData(responseDTO);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentArticleResponseDTO>>> getAllComments(
            @RequestParam Long id) {
        List<CommentArticleResponseDTO> responseDTOs = commentService.getAllComments(id);
        ApiResponse apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Fetched all comments");
        apiResponse.setData(responseDTOs);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);

    }
}
