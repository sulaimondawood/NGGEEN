package com.dawood.nggeen.shared.dto;


public record Meta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {

}
