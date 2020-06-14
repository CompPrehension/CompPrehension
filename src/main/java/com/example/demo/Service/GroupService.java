package com.example.demo.Service;

import com.example.demo.models.Dao.GroupDao;
import com.example.demo.models.entities.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupService {
    private GroupDao groupDao;

    @Autowired
    public GroupService(GroupDao groupDao) {
        this.groupDao = groupDao;
    }
}
