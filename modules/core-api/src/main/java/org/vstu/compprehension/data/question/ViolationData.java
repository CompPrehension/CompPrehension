package org.vstu.compprehension.data.question;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.enums.InteractionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Хранится в JSON рассуждений взаимодействия: пустые поля (номер, тип взаимодействия) туда не пишутся.
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationData implements Serializable {
    private Long id;
    private String lawName;
    private InteractionType interactionType;
    @Builder.Default
    private List<BackendFactData> violationFacts = new ArrayList<>();
    @Builder.Default
    private List<ExplanationTemplateInfoData> explanationTemplateInfo = new ArrayList<>();
}
