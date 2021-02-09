package com.example.demo.models.entities.QuestionOptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
public class QuestionOptionsEntity implements Serializable {
    private boolean requireContext;
    private boolean showTrace;
}
