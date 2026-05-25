package com.articlemanager.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.articlemanager.backend.entity.Articles;

public interface ArticleRepository extends JpaRepository<Articles, Long> {
    Optional<Articles> findBySlug(String slug);

    List<Articles> findBySlugStartingWith(String slugPrefix);
}