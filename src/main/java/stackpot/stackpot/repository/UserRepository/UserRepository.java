package stackpot.stackpot.repository.UserRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import stackpot.stackpot.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);
}
