package com.skillgap.navigator.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillgap.navigator.entity.Course;

public interface CourseRepository extends JpaRepository<Course,Long>{

    List<Course> findBySkill(String skill);

}