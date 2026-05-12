package com.articlemanager.backend.Utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.articlemanager.backend.Repository.ArticleRepository;
import com.articlemanager.backend.entity.Articles;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SlugUtil {

    private final ArticleRepository articleRepository;

    public String generateSlug(String inputString) {

        // 1. Normalize String
        String baseSlug = normalize(inputString);

        // 2. Fetch existing slugs
        List<Articles> existingArticles = articleRepository.findBySlugStartingWith(baseSlug);

        if (existingArticles.isEmpty()) {
            return baseSlug;
        }

        // 3. Extract existing slug values
        Set<String> existingSlugs = existingArticles.stream()
                .map(Articles::getSlug)
                .collect(Collectors.toSet());

        // 4. Find the next available suffix
        int counter = 1;
        String newSlug = baseSlug + "-" + counter;

        while (existingSlugs.contains(newSlug)) {
            counter++;
            newSlug = baseSlug + "-" + counter;
        }

        return newSlug;
    }

    // Helper function
    private String normalize(String input) {
        return input
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
