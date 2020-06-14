package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExplanationTemplateInfo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExplanationTemplateInfoDao extends CrudRepository<ExplanationTemplateInfo, Long> {
    Optional<ExplanationTemplateInfo> findExplanationTemplateInfoById(Long aLong);
}
