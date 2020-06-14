package com.example.demo.Service;

import com.example.demo.models.Dao.UserCourseRoleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCourseRoleService {
    private UserCourseRoleDao userCourseRoleDao;

    @Autowired
    public UserCourseRoleService(UserCourseRoleDao userCourseRoleDao) {
        this.userCourseRoleDao = userCourseRoleDao;
    }


}
