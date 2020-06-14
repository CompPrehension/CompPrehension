package com.example.demo.Service;

import com.example.demo.models.Dao.ExplanationTemplateInfoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExplanationTemplateInfoService {
    private ExplanationTemplateInfoDao explanationTemplateInfoDao;

    @Autowired
    public ExplanationTemplateInfoService(ExplanationTemplateInfoDao explanationTemplateInfoDao) {
        this.explanationTemplateInfoDao = explanationTemplateInfoDao;
    }
}
