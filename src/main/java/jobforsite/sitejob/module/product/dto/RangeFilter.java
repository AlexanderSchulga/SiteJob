package jobforsite.sitejob.module.product.dto;

import lombok.Data;

/**
 * DTO для хранения диапазонов значений.
 */
@Data
public class RangeFilter {
    private Integer min;
    private Integer max;

    public RangeFilter(Integer min, Integer max) {
        this.min = min;
        this.max = max;
    }

    public boolean isEmpty() {
        return min == null && max == null;
    }
}