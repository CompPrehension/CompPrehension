package com.example.demo.models.repository;

import com.example.demo.models.entities.ExplanationTemplateInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationTemplateInfoRepository extends CrudRepository<ExplanationTemplateInfo, Long> {
}
