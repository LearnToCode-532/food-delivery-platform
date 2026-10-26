package com.fooddelivery.catalog.config;

import com.fooddelivery.catalog.exception.InvalidCatalogParameterException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class CatalogPagination {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private CatalogPagination() {
    }

    public static Pageable create(
            int page,
            int size,
            String sortBy,
            String direction,
            Set<String> allowedFields) {
        if (page < 0) {
            throw new InvalidCatalogParameterException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidCatalogParameterException("size must be between 1 and " + MAX_SIZE);
        }
        if (!allowedFields.contains(sortBy)) {
            throw new InvalidCatalogParameterException("Unsupported sort field: " + sortBy);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCatalogParameterException("direction must be 'asc' or 'desc'");
        }

        Sort sort = Sort.by(sortDirection, sortBy);
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}
