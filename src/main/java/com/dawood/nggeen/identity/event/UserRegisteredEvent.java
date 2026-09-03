package com.dawood.nggeen.identity.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@Getter
public class UserRegisteredEvent {
    private String email;
    private String token;
    private String name;
}
