package com.articlemanager.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.articlemanager.backend.entity.Ratings;

public interface RatingRepository extends JpaRepository<Ratings, Long> {
    boolean existsByUserIdAndArticlesId(Long userId, Long articleId);
}
