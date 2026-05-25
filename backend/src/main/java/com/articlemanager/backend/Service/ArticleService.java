package com.articlemanager.backend.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.articlemanager.backend.DTOs.Request.ArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.ArticleResponseDTO;
import com.articlemanager.backend.Exception.ResourceNotFoundException;
import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.Utils.RatingCalculatorUtil;
import com.articlemanager.backend.Utils.SlugUtil;
import com.articlemanager.backend.entity.Articles;
import com.articlemanager.backend.entity.User;

import jakarta.transaction.Transactional;
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
        log.info("articles.list.fetched count={}", articles.size());
        return articles;
    }

    public ArticleResponseDTO getArticleById(Long articleId) {
        log.info("article.fetch.request_received articleId={}", articleId);
        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));

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

    @Transactional
    public ArticleResponseDTO addArticle(ArticleRequestDTO requestDTO) {

        log.debug("article.create.payload_received payload={}", requestDTO); // Fetch the author from author id
        User author = userRepository.findById(requestDTO.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author", requestDTO.getAuthorId()));

        // generate slug
        String slug = slugUtil.generateSlug(requestDTO.getHeading());
        log.info("articles.slug.generated slug={} heading={}", slug, requestDTO.getHeading());

        Articles article = new Articles();
        article.setHeading(requestDTO.getHeading());
        article.setContent(requestDTO.getContent());
        article.setAuthor(author);
        article.setSlug(slug);

        Articles savedArticle = articleRepository.save(article);
        log.info(
                "article.create.saved articleId={} authorId={}",
                article.getId(),
                article.getAuthor().getId());

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

    @Transactional
    public BigDecimal updateRating(Long articleId, Short newRating) {
        log.info(
                "article.rating_update.started articleId={} newRating={}",
                articleId,
                newRating);

        Articles article = articleRepository.findById(articleId)
                .orElseThrow(() -> {
                    log.warn(
                            "article.rating_update.article_not_found articleId={}",
                            articleId);

                    return new ResourceNotFoundException("Article", articleId);
                });

        log.debug(
                "article.rating_update.current_state articleId={} currentAverage={} currentCount={}",
                articleId,
                article.getAverageRatings(),
                article.getRatingCount());

        BigDecimal updatedRating = ratingCalculatorUtil.calculateRating(article.getAverageRatings(),
                article.getRatingCount(), newRating);

        article.setAverageRatings(updatedRating);
        article.setRatingCount(article.getRatingCount() + 1);

        log.info(
                "article.rating_update.success articleId={} updatedAverage={} updatedCount={}",
                articleId,
                updatedRating,
                article.getRatingCount());

        return updatedRating;

    }
}
