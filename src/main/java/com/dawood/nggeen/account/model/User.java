package com.dawood.nggeen.account.model;

import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class User extends MetaData {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullname;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserStatus status;

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<Account> account = new ArrayList<>();

    public static User create(String email, String fullname, String passwordHash, UserStatus status) {
        User user = new User();
        user.email = email;
        user.fullname = fullname;
        user.passwordHash = passwordHash;
        user.status = status;
        return user;
    }

    public boolean canTrade() {
        return this.status == UserStatus.ACTIVE;
    }
}
