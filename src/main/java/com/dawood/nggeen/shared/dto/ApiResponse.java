package com.dawood.nggeen.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponse<T> {
    private final T data;
    private final String message;
    private final Meta meta;

    private ApiResponse(T data, String message, Meta meta){
       this.data=data;
       this.message=message;
       this.meta=meta;
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(data, "Operation Successful", null);
    }

    public static <T> ApiResponse<T> successMessage(String message){
        return new ApiResponse<>(null, message, null);
    }

    public static <T> ApiResponse<T> success(T data, String message){
        return new ApiResponse<>(data, message, null);
    }

    public static <T> ApiResponse<T> success(T data, String message, Meta meta){
        return new ApiResponse<>(data, message, meta);
    }



}
