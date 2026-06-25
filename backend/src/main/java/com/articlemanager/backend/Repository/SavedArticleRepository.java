package com.articlemanager.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.articlemanager.backend.entity.SavedArticle;

public interface SavedArticleRepository extends JpaRepository<SavedArticle, Long> {
    boolean existsByUserIdAndArticlesId(Long userId, Long articleId);

    Optional<SavedArticle> findByUserIdAndArticlesId(Long userId, Long articleId);

    List<SavedArticle> findAllByUserId(Long userId);
}
