package stackpot.stackpot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import stackpot.stackpot.domain.common.BaseEntity;
import stackpot.stackpot.domain.enums.Role;

import java.util.Collection;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
//@Where(clause = "is_deleted = false")
public class User extends BaseEntity implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary Key

    @Column(nullable = true, length = 255)
    private String nickname; // 닉네임

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 255)
    private Role role; // 역할

    @Column(nullable = true, length = 255)
    private String interest; // 관심사

    @Column(nullable = true, columnDefinition = "TEXT")
    private String userIntroduction; // 한 줄 소개

    @Getter
    @Setter
    @Column(nullable = true)
    private Integer userTemperature; // 유저 온도

    @Column(nullable = true, unique = true)
    private String email; // 이메일

    @Column(nullable = true, unique = true)
    private String kakaoId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10)
    private List<Pot> pots;

    private boolean isDeleted = false; // 삭제 여부 필드

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email; // 사용자 식별자로 이메일을 사용
    }
    public Long getUserId() {
        return id;
    }

    public void deleteUser() {
        this.isDeleted = true;
        this.nickname = "알 수 없음";  // 표시용 변경
//        this.role = Role.UNKNOWN;
        this.kakaoId = null;
        this.interest = "UNKNOWN";
        this.userTemperature = null;
        this.email=null;
        // 이메일 등 개인 정보 삭제
    }

}

