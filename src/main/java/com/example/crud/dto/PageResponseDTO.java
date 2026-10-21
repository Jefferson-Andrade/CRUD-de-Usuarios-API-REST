package com.example.crud.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponseDTO<T>(List<T> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
    public static <T> PageResponseDTO<T> from(Page<T> result) {
        return new PageResponseDTO<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
