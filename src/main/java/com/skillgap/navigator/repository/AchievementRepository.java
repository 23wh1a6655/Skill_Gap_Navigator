package com.skillgap.navigator.repository;

import com.skillgap.navigator.entity.Achievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findTop8ByUserIdOrderByIssuedAtDesc(Long userId);

    boolean existsByUserIdAndTitleIgnoreCase(Long userId, String title);
}
