package com.articlemanager.backend.DTOs.Response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateArticleResponseDTO {

    private Integer id;

    private BigDecimal rating;

    private Integer totalRatings;
}
