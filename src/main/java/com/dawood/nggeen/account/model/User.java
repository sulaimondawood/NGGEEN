package com.dawood.nggeen.account.model;

import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User extends MetaData {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserStatus status;

    public User(String email, String username, String password, UserStatus status) {
        this.email = email;
        this.username = username;
        this.passwordHash = password;
        this.status = status;
    }

    public boolean canTrade() {
        return this.status == UserStatus.ACTIVE;
    }
}
