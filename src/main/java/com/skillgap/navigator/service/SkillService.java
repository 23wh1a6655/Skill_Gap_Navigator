package com.skillgap.navigator.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillgap.navigator.entity.RoleSkill;
import com.skillgap.navigator.repository.RoleSkillRepository;

@Service
public class SkillService {

    @Autowired
    private RoleSkillRepository roleSkillRepository;

    public List<RoleSkill> getSkillsByRole(String role) {
        return roleSkillRepository.findByRole(role);
    }
}