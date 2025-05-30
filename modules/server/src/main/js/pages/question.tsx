import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { container } from "tsyringe";
import DebugButton from "../components/common/debug";
import { GenerateNextAnswerBtn } from "../components/exercise/generate-next-answer-btn";
import { Question } from "../components/exercise/question";
import { QuestionStore } from "../stores/question-store";

export const QuestionPage = observer(() => {
    const [question] = useState(() => container.resolve(QuestionStore));

    // load question based on metadataId from the URL    
    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const metadataId = urlParams.get('metadataId');
        if (!metadataId || Number.isNaN(Number(metadataId))) {
            throw new Error("no metadataId provided in the URL");
        }
        question.generateQuestionByMetadata(+metadataId);       
    }, [question]);


    return (
        <>
            <Question store={question} showExtendedFeedback />
            <div className="mt-3 position-relative">
                <GenerateNextAnswerBtn store={question}/>
                <DebugButton metadataId={question.question?.questionMetadataId ?? -1}/> 
            </div>
        </>
    );
});
