package com.articlemanager.backend.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.articlemanager.backend.entity.Articles;

public interface ArticleRepository extends JpaRepository<Articles, Long> {
    Optional<Articles> findBySlug(String slug);

    List<Articles> findBySlugStartingWith(String slugPrefix);

    @Query(value = """
            SELECT * FROM articles
            WHERE status = 'PUBLISHED'
            AND search_vector @@ websearch_to_tsquery('english',:query)
            ORDER BY ts_rank(search_vector, websearch_to_tsquery('english', :query)) DESC
            """, nativeQuery = true)
    List<Articles> searchPublishedArticles(@Param("query") String query);
}