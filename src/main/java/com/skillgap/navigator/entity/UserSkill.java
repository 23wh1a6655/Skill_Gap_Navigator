package com.skillgap.navigator.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_skills")
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String roleName;

    @Column(nullable = false)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel targetLevel;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public SkillLevel getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(SkillLevel currentLevel) {
        this.currentLevel = currentLevel;
    }

    public SkillLevel getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(SkillLevel targetLevel) {
        this.targetLevel = targetLevel;
    }
}
