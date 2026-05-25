package com.articlemanager.backend.Service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.articlemanager.backend.DTOs.Request.RateArticleRequestDTO;
import com.articlemanager.backend.DTOs.Response.RateArticleResponseDTO;
import com.articlemanager.backend.Exception.DuplicateResourceException;
import com.articlemanager.backend.Exception.ResourceNotFoundException;
import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.Repository.RatingRepository;
import com.articlemanager.backend.Repository.UserRepository;
import com.articlemanager.backend.entity.Articles;
import com.articlemanager.backend.entity.Ratings;
import com.articlemanager.backend.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {
        private final ArticleRepository articleRepository;
        private final UserRepository userRepository;
        private final RatingRepository ratingRepository;
        private final ArticleService articleService;
        private static final Logger log = LoggerFactory.getLogger(RatingService.class);

        @Transactional
        public RateArticleResponseDTO rateArticle(RateArticleRequestDTO requestDTO) {
                log.info("user id - {} has given {} rating for article id -{}", requestDTO.getUserId(),
                                requestDTO.getRating(),
                                requestDTO.getArticleId());

                if (ratingRepository.existsByUserIdAndArticlesId(requestDTO.getUserId(), requestDTO.getArticleId()))
                        throw new DuplicateResourceException("User has already rated this article");

                Articles article = articleRepository.findById(requestDTO.getArticleId())
                                .orElseThrow(() -> new ResourceNotFoundException("Article", requestDTO.getArticleId()));

                User author = userRepository.findById(requestDTO.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("Author", requestDTO.getUserId()));

                Ratings rate = new Ratings();
                rate.setArticles(article);
                rate.setUser(author);
                rate.setRating(requestDTO.getRating());

                ratingRepository.save(rate);

                BigDecimal newRate = articleService.updateRating(requestDTO.getArticleId(), requestDTO.getRating());

                RateArticleResponseDTO responseDTO = new RateArticleResponseDTO();
                responseDTO.setRating(newRate);
                responseDTO.setTotalRatings(null);

                return responseDTO;
        }
}
