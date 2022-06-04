import React, { useCallback, useEffect, useState } from "react";
import { container } from "tsyringe";
import { SurveyController } from "../../controllers/exercise/survey-controller";
import { Survey, YesNoSurveyQuestion } from "../../types/survey";
import * as E from "fp-ts/lib/Either";

export type SurveyComponentProps = {
    surveyId : string;
    questionId: number;
    onCompleted: () => void,
}
export const SurveyComponent = (props: SurveyComponentProps) => {
    var [endpoint] = useState(() => container.resolve(SurveyController));
    var [survey, setSurvey] = useState<Survey>();
    var [surveyState, setSurveyState] = useState<'INITAL' | 'SENDING_RESULTS' | 'COMPLETED'>('INITAL');
    var [surveyAnswers, setSurveyAnswers] = useState<Record<number, string>>({});

    useEffect(() => {
        (async () => {
            var survey = await endpoint.getSurvey(props.surveyId);
            if (E.isRight(survey))
                setSurvey(survey.right);
        })();
    }, []);

    var onAnswered = useCallback((questionId: number, answer: string) => {
        (async () => {
            const newAnswers = {...surveyAnswers, [questionId]: answer};
            setSurveyAnswers(newAnswers);
            if (survey?.questions.length === Object.getOwnPropertyNames(newAnswers).length) {
                setSurveyState('SENDING_RESULTS');                
                await Promise.all(survey!.questions.map(q => endpoint.postSurveyAnswer(q.id, props.questionId, newAnswers[q.id])));
                setSurveyState('COMPLETED');
                props.onCompleted();
            }
        })();
    }, [survey]);

    if (!survey || !survey.questions || !survey.questions.length || surveyState === 'COMPLETED')
        return null;

    return (
        <div className="alert alert-warning rounded" role="alert">
            {survey.questions
                .filter(q => surveyAnswers[q.id] === undefined)
                .map(q => 
                    q.type === 'yes-no' && <SurveyYesNoQuestion question={q} onAnswered={onAnswered}/>
                    || "invalid question type")}
        </div>
    );
}

type SurveyYesNoQuestionProps = {
    question: YesNoSurveyQuestion,
    onAnswered: (questionId: number, answer: string) => void,
}
const SurveyYesNoQuestion = (props: SurveyYesNoQuestionProps) => {
    const { question, onAnswered } = props;
    return (
        <div >
            <div className="mb-1">
                {question.text}
            </div>            
            <div className="d-flex flex-row mt-2">
                <button type="button" className="btn btn-secondary mr-2" onClick={() => onAnswered(question.id, question.options.yesValue)}>
                    {question.options.yesText}
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => onAnswered(question.id, question.options.noValue)}>
                    {question.options.noText}
                </button>
            </div>
        </div>        
    )
}