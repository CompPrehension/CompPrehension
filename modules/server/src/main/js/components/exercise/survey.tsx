import React, { useCallback, useEffect, useState } from "react";
import { container } from "tsyringe";
import { SurveyController } from "../../controllers/exercise/survey-controller";
import { OpenEndedSurveyQuestion, SingleChoiceSurveyQuestion, Survey, SurveyQuestion, YesNoSurveyQuestion } from "../../types/survey";
import * as E from "fp-ts/lib/Either";
import { Loader } from "../common/loader";
import { Button, Form } from "react-bootstrap";
import { Optional } from "../common/optional";
import { useTranslation } from "react-i18next";

export type SurveyComponentProps = {
    survey: Survey;
    enabledSurveyQuestions: number[];
    value?: Record<number, string>;
    isCompleted?: boolean,
    questionId: number;
    onAnswersSended: (survey: Survey, questionId: number, answers: Record<number, string>) => void,
}
export const SurveyComponent = (props: SurveyComponentProps) => {
    const { survey, enabledSurveyQuestions, isCompleted } = props;
    const [endpoint] = useState(() => container.resolve(SurveyController));
    const [surveyState, setSurveyState] = useState<'INITAL' | 'VALIDATION_ERROR' | 'SENDING_RESULTS' | 'COMPLETED'>(isCompleted ? 'COMPLETED' : 'INITAL');
    const [surveyAnswers, setSurveyAnswers] = useState<Record<number, string>>(props.value || {});    
    const { t } = useTranslation();
    const surveyQuestions = props.survey.questions.filter(q => enabledSurveyQuestions.includes(q.id))

    var onAnswered = (questionId: number, answer: string) => {        
        const newAnswers = { ...surveyAnswers, [questionId]: answer };
        setSurveyAnswers(newAnswers);
        console.log(newAnswers);
    };

    const sendAnswers = () => {
        (async () => {
            const requiredQuestionIds = surveyQuestions.filter(x => x.required).map(x => x.id);
            if (requiredQuestionIds.every(id => surveyAnswers[id])) {
                setSurveyState('SENDING_RESULTS');
                await Promise.all(surveyQuestions
                    .map(q => endpoint.postSurveyAnswer(q.id, props.questionId, surveyAnswers[q.id])));
                setSurveyState('COMPLETED');
                props.onAnswersSended(survey, props.questionId, surveyAnswers);
            } else {
                setSurveyState('VALIDATION_ERROR');
            }
        })();
    };

    if (!surveyQuestions || !surveyQuestions.length /*|| surveyState === 'COMPLETED'*/)
        return null;

    //if (surveyState === 'SENDING_RESULTS')
    //   return <Loader delay={100} />;

    return (
        <div className="alert alert-warning rounded" role="alert">
            {surveyQuestions
                .map((q, idx, qs) =>
                    <div key={idx} className={idx !== qs.length - 1 && "mb-4" || ''}>
                        {q.type === 'yes-no' && <SurveyYesNoQuestion isCompleted={isCompleted} question={q} onAnswered={onAnswered} value={surveyAnswers[q.id]} />
                            || q.type === 'single-choice' && <SingleChoiceSurveyQuestionComponent isCompleted={isCompleted} question={q} onAnswered={onAnswered} value={surveyAnswers[q.id]} />
                            || q.type === 'open-ended' && <OpenEndedSurveyQuestionComponent isCompleted={isCompleted} question={q} onAnswered={onAnswered} value={surveyAnswers[q.id]} />
                            || "invalid question type"
                        }
                    </div>
                )}
            {surveyState === 'SENDING_RESULTS' && 
                <div className="mt-2">
                    <Loader delay={100} />
                </div>}
            {surveyState === 'VALIDATION_ERROR' &&
                <div className="mt-2">
                    <div className="alert alert-danger rounded">
                        Необходимо ответить на все обязательные вопросы
                    </div>
                </div>}
            {surveyState !== 'COMPLETED' && surveyState !== 'SENDING_RESULTS' && 
                <div className="mt-2">
                    <Button variant="primary" onClick={sendAnswers}>{t('survey_sendresults')}</Button>
                </div>}
        </div>
    );
}

type SurveyYesNoQuestionProps = {
    question: YesNoSurveyQuestion,
    isCompleted?: boolean,
    value?: string;
    onAnswered: (questionId: number, answer: string) => void,
}
const SurveyYesNoQuestion = (props: SurveyYesNoQuestionProps) => {
    const { question, onAnswered, value, isCompleted } = props;
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

type SurveySigleChoiceQuestionProps = {
    question: SingleChoiceSurveyQuestion,
    isCompleted?: boolean,
    value?: string;
    onAnswered: (questionId: number, answer: string) => void,
}
export const SingleChoiceSurveyQuestionComponent = (props: SurveySigleChoiceQuestionProps) => {
    const { question, onAnswered, value, isCompleted } = props;

    return (
        <div>
            <div className="mb-1">
                {question.text}
            </div>
            <div className="d-flex mt-2">
                <div>
                    {question.options.map((o, idx, opts) =>
                        <div key={o.id}>
                            <Form.Check
                                disabled={isCompleted}
                                checked={value === o.id}
                                name={`radio_qid${question.id}`}
                                type={'radio'}
                                id={`radio_qid${question.id}_sqid${o.id}`}
                                label={o.text}
                                value={o.id}
                                onChange={e => onAnswered(question.id, e.target.value)}
                            />
                        </div>)}
                </div>

            </div>
        </div>
    )
}

type SurveyOpenEndedQuestionProps = {
    question: OpenEndedSurveyQuestion,
    value?: string;
    isCompleted?: boolean,
    onAnswered: (questionId: number, answer: string) => void,
}
export const OpenEndedSurveyQuestionComponent = (props: SurveyOpenEndedQuestionProps) => {
    const { question, onAnswered, value, isCompleted } = props;
    return (
        <div>
            <div className="mb-1">
                {question.text}
            </div>
            <div className="d-flex flex-row mt-2">
                <Form.Control
                    disabled={isCompleted}
                    value={value}
                    as="textarea"
                    rows={3} 
                    onChange={e => onAnswered(question.id, e.target.value)}/>
            </div>
        </div>
    )
}
