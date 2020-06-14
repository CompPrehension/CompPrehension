package com.example.demo.Service;

import com.example.demo.models.entities.User;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User getUserByEmail(String email){
        try{
            return userDao.findUserByEmail(email).orElseThrow(()->new UserNFException("User with id" + email + "Not Found"));
        }
        catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }
}
