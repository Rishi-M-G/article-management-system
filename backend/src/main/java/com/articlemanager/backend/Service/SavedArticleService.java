package com.articlemanager.backend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.articlemanager.backend.DTOs.Response.ArticleResponseDTO;
import com.articlemanager.backend.Exception.ResourceNotFoundException;
import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.Repository.SavedArticleRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.entity.Articles;
import com.articlemanager.backend.entity.SavedArticle;
import com.articlemanager.backend.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedArticleService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final SavedArticleRepository savedArticleRepository;

    @Transactional
    public boolean saveArticle(Long articleId, Long userId) {

        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", userId));

        if (savedArticleRepository.existsByUserIdAndArticlesId(userId, articleId)) {
            return false;
        }
        SavedArticle savedArticle = new SavedArticle();
        savedArticle.setArticles(article);
        savedArticle.setUser(author);
        savedArticleRepository.save(savedArticle);
        return true;
    }

    @Transactional
    public void unSaveArticle(Long articleId, Long userId) {
        SavedArticle savedArticle = savedArticleRepository.findByUserIdAndArticlesId(userId, articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved Article", userId));

        savedArticleRepository.delete(savedArticle);
    }

    public List<ArticleResponseDTO> getSavedArticles(Long userId) {

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", userId));

        List<SavedArticle> savedArticles = savedArticleRepository.findAllByUserId(userId);

        List<ArticleResponseDTO> responseDTOs = new ArrayList<>();

        for (SavedArticle savedArticle : savedArticles) {

            Articles article = articleRepository.getReferenceById(savedArticle.getArticles().getId());
            ArticleResponseDTO responseDTO = new ArticleResponseDTO();
            responseDTO.setId(article.getId());
            responseDTO.setHeading(article.getHeading());
            responseDTO.setContent(article.getContent());
            responseDTO.setSummary(article.getSummary());
            responseDTO.setSlug(article.getSlug());
            responseDTO.setAuthorName(article.getAuthor().getFirstName());
            responseDTO.setStatus(article.getStatus());
            responseDTO.setCreatedAt(article.getCreatedAt());
            responseDTO.setUpdatedAt(article.getUpdatedAt());

            responseDTOs.add(responseDTO);
        }
        return responseDTOs;
    }
}
