package com.example.demo.Service;

import com.example.demo.models.Dao.LawFormulationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LawFormulationService {
    private LawFormulationDao lawFormulationDao;

    @Autowired
    public LawFormulationService(LawFormulationDao lawFormulationDao) {
        this.lawFormulationDao = lawFormulationDao;
    }
}
