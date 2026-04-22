package com.skillgap.navigator.repository;

import com.skillgap.navigator.entity.QuizAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findTop5ByUserIdOrderByCompletedAtDesc(Long userId);

    List<QuizAttempt> findByUserIdAndSkillNameIgnoreCaseOrderByCompletedAtDesc(Long userId, String skillName);
}
