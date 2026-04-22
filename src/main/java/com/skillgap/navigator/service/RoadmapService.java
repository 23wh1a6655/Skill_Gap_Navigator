package com.skillgap.navigator.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillgap.navigator.entity.Course;
import com.skillgap.navigator.repository.CourseRepository;

@Service
public class RoadmapService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> generateRoadmap(List<String> missingSkills){

        List<Course> roadmap = new ArrayList<>();

        for(String skill : missingSkills){

            List<Course> courses = courseRepository.findBySkill(skill);

            roadmap.addAll(courses);
        }

        return roadmap;
    }
}