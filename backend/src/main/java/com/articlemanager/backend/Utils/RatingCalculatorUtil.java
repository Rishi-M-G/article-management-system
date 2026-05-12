package com.articlemanager.backend.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RatingCalculatorUtil {

    public BigDecimal calculateRating(BigDecimal averageRating, Integer count, Short newRate) {

        BigDecimal avg = averageRating; // BigDecimal
        BigDecimal cnt = BigDecimal.valueOf(count); // convert int → BigDecimal
        BigDecimal incoming = BigDecimal.valueOf(newRate);
        // totalSum = avg * count
        BigDecimal totalSum = avg.multiply(cnt);

        // updatedSum = totalSum + newRating
        BigDecimal updatedSum = totalSum.add(incoming);

        // newAvg = updatedSum / (count + 1)
        BigDecimal newAvg = updatedSum.divide(
                cnt.add(BigDecimal.ONE),
                2, // scale (e.g., 2 decimal places)
                RoundingMode.HALF_UP);

        return newAvg;
    }
}
