package com.skillgap.navigator.repository;

import com.skillgap.navigator.entity.UserSkill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUserIdOrderBySkillNameAsc(Long userId);

    void deleteByUserId(Long userId);
}
