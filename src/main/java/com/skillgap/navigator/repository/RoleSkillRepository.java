package com.skillgap.navigator.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.skillgap.navigator.entity.RoleSkill;

public interface RoleSkillRepository extends JpaRepository<RoleSkill, Long>{

    List<RoleSkill> findByRole(String role);

}