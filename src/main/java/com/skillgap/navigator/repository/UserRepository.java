package com.skillgap.navigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.skillgap.navigator.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

}
