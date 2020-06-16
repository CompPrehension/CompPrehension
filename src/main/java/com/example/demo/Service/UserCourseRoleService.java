package com.example.demo.Service;

import com.example.demo.models.Dao.UserCourseRoleDao;
import com.example.demo.models.entities.UserAction;
import com.example.demo.models.entities.UserCourseRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCourseRoleService {
    private UserCourseRoleDao userCourseRoleDao;

    @Autowired
    public UserCourseRoleService(UserCourseRoleDao userCourseRoleDao) {
        this.userCourseRoleDao = userCourseRoleDao;
    }

    public void saveUserCourseRole(UserCourseRole userCourseRole) {

        userCourseRoleDao.save(userCourseRole);
    }
}
