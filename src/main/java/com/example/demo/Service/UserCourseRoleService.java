package com.example.demo.Service;

import com.example.demo.models.repository.UserCourseRoleRepository;
import com.example.demo.models.entities.UserCourseRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCourseRoleService {
    private UserCourseRoleRepository userCourseRoleRepository;

    @Autowired
    public UserCourseRoleService(UserCourseRoleRepository userCourseRoleRepository) {
        this.userCourseRoleRepository = userCourseRoleRepository;
    }

    public void saveUserCourseRole(UserCourseRole userCourseRole) {

        userCourseRoleRepository.save(userCourseRole);
    }
}
