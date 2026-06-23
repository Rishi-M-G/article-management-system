package com.articlemanager.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.articlemanager.backend.entity.Comments;

public interface CommentRepository extends JpaRepository<Comments, Long> {
    boolean existsByUserIdAndArticlesId(Long userId, Long articleId);

    List<Comments> findAllByArticlesId(Long articleId);
}
