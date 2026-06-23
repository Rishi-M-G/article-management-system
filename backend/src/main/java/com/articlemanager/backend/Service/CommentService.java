package com.articlemanager.backend.Service;

import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.Repository.CommentRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.Service.ArticleService;
import com.articlemanager.backend.entity.Articles;
import com.articlemanager.backend.entity.Comments;
import com.articlemanager.backend.entity.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.articlemanager.backend.DTOs.Request.CommentArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.CommentArticleResponseDTO;
import com.articlemanager.backend.Exception.DuplicateResourceException;
import com.articlemanager.backend.Exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ArticleService articleService;
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentArticleResponseDTO commentArticle(CommentArticleRequestDTO requestDTO) {

        if (commentRepository.existsByUserIdAndArticlesId(requestDTO.getUserId(), requestDTO.getArticleId()))
            throw new DuplicateResourceException("User has already commented this article");

        Articles article = articleRepository.findById(requestDTO.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException("Article", requestDTO.getArticleId()));

        User author = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", requestDTO.getUserId()));

        Comments comments = new Comments();
        comments.setArticles(article);
        comments.setUser(author);
        comments.setContent(requestDTO.getComment());

        commentRepository.save(comments);

        // update comment count
        BigDecimal commentCount = articleService.updateCommentsCount(requestDTO.getArticleId());

        CommentArticleResponseDTO responseDTO = new CommentArticleResponseDTO();
        responseDTO.setComment(comments.getContent());
        responseDTO.setId(comments.getId());
        responseDTO.setTotalComments(commentCount.intValue());

        return responseDTO;
    }

    public List<CommentArticleResponseDTO> getAllComments(Long articleId) {
        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));

        List<Comments> allComments = commentRepository.findAllByArticlesId(articleId);
        List<CommentArticleResponseDTO> responseDTOs = new ArrayList<>();
        for (Comments comments : allComments) {
            CommentArticleResponseDTO responseDTO = new CommentArticleResponseDTO();
            responseDTO.setComment(comments.getContent());
            responseDTO.setId(comments.getId());
            responseDTOs.add(responseDTO);
        }
        return responseDTOs;
    }
}
