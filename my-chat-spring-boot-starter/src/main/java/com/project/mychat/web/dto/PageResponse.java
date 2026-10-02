package com.project.mychat.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A self-owned, stable JSON shape for paginated results — deliberately not Spring Data's raw
 * {@code Page} (whose JSON serialization Spring itself documents as unstable across versions),
 * since this is a versioned API other products integrate against.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
