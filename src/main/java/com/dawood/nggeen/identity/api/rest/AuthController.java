package com.dawood.nggeen.identity.api.rest;

import com.dawood.nggeen.identity.api.rest.dto.CreateUserRequest;
import com.dawood.nggeen.identity.api.rest.dto.CreateUserResponse;
import com.dawood.nggeen.identity.application.AuthApplicationService;
import com.dawood.nggeen.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthApplicationService applicationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CreateUserResponse>> register(@Valid @RequestBody CreateUserRequest request){
      CreateUserResponse response = applicationService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response,
                "Account created successfully. Please check your email to verify your address."));

    }

}
