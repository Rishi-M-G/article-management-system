package com.articlemanager.backend.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.articlemanager.backend.DTOs.Request.ArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ArticleResponseDTO;
import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.Utils.RatingCalculatorUtil;
import com.articlemanager.backend.Utils.SlugUtil;
import com.articlemanager.backend.entity.Articles;
import com.articlemanager.backend.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    // Import dependencies
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final SlugUtil slugUtil;
    private final RatingCalculatorUtil ratingCalculatorUtil;
    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    public List<Articles> getAllArticles() {
        List<Articles> articles = articleRepository.findAll();
        log.info("🔥🔥🔥 All articles are fetched");
        return articles;
    }

    public ArticleResponseDTO getArticleById(Integer articleId) {
        log.info("❗❗❗ Received request to fetch article {}", articleId);
        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

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

        return responseDTO;
    }

    public ArticleResponseDTO addArticle(ArticleRequestDTO requestDTO) {

        log.debug("❗❗❗ Add Article Payload Received : {}", requestDTO);
        // Fetch the author from author id
        User author = userRepository.findById(requestDTO.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Author not found"));

        // generate slug
        String slug = slugUtil.generateSlug(requestDTO.getHeading());
        log.info("🔥🔥🔥 New slug {} created for the received payload {}", slug, requestDTO);

        Articles article = new Articles();
        article.setHeading(requestDTO.getHeading());
        article.setContent(requestDTO.getContent());
        article.setAuthor(author);
        article.setSlug(slug);

        Articles savedArticle = articleRepository.save(article);
        log.info("🔥🔥🔥 New article {} saved in database", article);

        ArticleResponseDTO responseDTO = new ArticleResponseDTO();
        responseDTO.setId(savedArticle.getId());
        responseDTO.setHeading(savedArticle.getHeading());
        responseDTO.setContent(savedArticle.getContent());
        responseDTO.setSummary(savedArticle.getSummary());
        responseDTO.setSlug(savedArticle.getSlug());
        responseDTO.setAuthorName(savedArticle.getAuthor().getFirstName());
        responseDTO.setStatus(savedArticle.getStatus());
        responseDTO.setCreatedAt(savedArticle.getCreatedAt());
        responseDTO.setUpdatedAt(savedArticle.getUpdatedAt());

        return responseDTO;
    }

    public BigDecimal updateRating(Integer articleId, Short newRating) {
        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        BigDecimal updatedRating = ratingCalculatorUtil.calculateRating(article.getAverageRatings(),
                article.getRatingCount(), newRating);

        article.setAverageRatings(updatedRating);
        article.setRatingCount(article.getRatingCount() + 1);

        return updatedRating;

    }
}
