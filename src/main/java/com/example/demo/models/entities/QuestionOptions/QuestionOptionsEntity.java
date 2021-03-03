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
/**
  Base class for question options
 */
public class QuestionOptionsEntity implements Serializable {
    /// Question text contains answers
    private boolean requireContext;
}
