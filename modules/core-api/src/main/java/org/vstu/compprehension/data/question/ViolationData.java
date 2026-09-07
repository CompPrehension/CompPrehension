package org.vstu.compprehension.data.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.enums.InteractionType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationData {
    private Long id;
    private String lawName;
    private String detailedLawName;
    private InteractionType interactionType;
    @Builder.Default
    private List<BackendFactData> violationFacts = new ArrayList<>();
    @Builder.Default
    private List<ExplanationTemplateInfoData> explanationTemplateInfo = new ArrayList<>();
}
