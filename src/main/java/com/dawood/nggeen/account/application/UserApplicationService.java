package com.dawood.nggeen.account.application;

import com.dawood.nggeen.account.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService {
    private final UserRepository userRepository;

    public void createUser(CreateUserRequest request){

    }
}
