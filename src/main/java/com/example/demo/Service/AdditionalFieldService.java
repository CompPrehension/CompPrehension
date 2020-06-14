package com.example.demo.Service;


import com.example.demo.models.Dao.AdditionalFieldDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdditionalFieldService {
    private AdditionalFieldDao additionalFieldDao;

    @Autowired
    public AdditionalFieldService(AdditionalFieldDao additionalFieldDao) {
        this.additionalFieldDao = additionalFieldDao;
    }
}
