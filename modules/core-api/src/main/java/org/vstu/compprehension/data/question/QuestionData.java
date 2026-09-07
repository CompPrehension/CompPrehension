package org.vstu.compprehension.data.question;

import org.vstu.compprehension.data.questionoptions.QuestionOptionsData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.QuestionStatus;
import org.vstu.compprehension.enums.QuestionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class QuestionData {
    private Long id;
    private QuestionType questionType;
    private QuestionStatus questionStatus;
    private String questionText;
    private String questionName;
    private Date createdAt;
    private @Nullable QuestionMetadataData metadata;
    private String questionDomainType;
    private QuestionOptionsData options;
    private @NotNull List<String> tags = new ArrayList<>(0);
    private List<AnswerObjectData> answerObjects = new ArrayList<>();
    private @NotNull List<QuestionInteractionData> interactions = new ArrayList<>(0);
    private List<BackendFactData> statementFacts = new ArrayList<>();
    private List<BackendFactData> solutionFacts = new ArrayList<>();
}
