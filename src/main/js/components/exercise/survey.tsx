import React, { useCallback, useEffect, useState } from "react";
import { container } from "tsyringe";
import { SurveyController } from "../../controllers/exercise/survey-controller";
import { Survey, YesNoSurveyQuestion } from "../../types/survey";
import * as E from "fp-ts/lib/Either";
import { Loader } from "../common/loader";

export type SurveyComponentProps = {
    surveyId : string;
    value?: Record<number, string>;
    questionId: number;
    onAnswered: (questionId: number, answers: Record<number, string>) => void,
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
                props.onAnswered(props.questionId, newAnswers);
            }
        })();
    }, [survey]);

    if (!survey || !survey.questions || !survey.questions.length || surveyState === 'COMPLETED')
        return null;
    
    if (surveyState === 'SENDING_RESULTS')
        return <Loader delay={100} />;

    return (
        <div className="alert alert-warning rounded" role="alert">
            {survey.questions
                .filter(q => surveyAnswers[q.id] === undefined)
                .map((q, idx) => 
                    q.type === 'yes-no' && <SurveyYesNoQuestion key={idx} question={q} onAnswered={onAnswered} value={props.value?.[q.id]}/>
                    || "invalid question type")}
        </div>
    );
}

type SurveyYesNoQuestionProps = {
    question: YesNoSurveyQuestion,
    value?: string;
    onAnswered: (questionId: number, answer: string) => void,
}
const SurveyYesNoQuestion = (props: SurveyYesNoQuestionProps) => {
    const { question, onAnswered, value } = props;
    return (
        <div>
            <div className="mb-1">
                {question.text}
            </div>            
            <div className="d-flex flex-row mt-2">
                <button type="button" 
                        className={`btn btn-secondary mr-2 ${question.options.yesValue === value ? "active" : ""}`}
                        onClick={() => onAnswered(question.id, question.options.yesValue)}
                        disabled={value !== undefined}>
                    {question.options.yesText}
                </button>
                <button type="button" 
                        className={`btn btn-secondary ${question.options.noValue === value ? "active" : ""}`} 
                        onClick={() => onAnswered(question.id, question.options.noValue)}
                        disabled={value !== undefined}>
                    {question.options.noText}
                </button>
            </div>
        </div>        
    )
}
