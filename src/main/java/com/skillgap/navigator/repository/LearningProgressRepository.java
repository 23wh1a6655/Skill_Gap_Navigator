package com.skillgap.navigator.repository;

import com.skillgap.navigator.entity.LearningProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {

    List<LearningProgress> findByUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<LearningProgress> findByUserIdAndSkillNameIgnoreCase(Long userId, String skillName);

    void deleteByUserId(Long userId);
}
