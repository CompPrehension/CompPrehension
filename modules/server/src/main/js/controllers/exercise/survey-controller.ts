import { injectable } from "tsyringe";
import { Survey, SurveyResultItem, TSurvey, TSurveyResultItem } from "../../types/survey";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";
import * as E from "fp-ts/lib/Either";
import { RequestError } from "../../types/request-error";
import * as io from 'io-ts';


@injectable()
export class SurveyController {
    private surveyCache: Record<string, Survey> = {};
    async getSurvey(suerveyId: string): PromiseEither<RequestError, Survey> {
        if (this.surveyCache[suerveyId])
            return E.right(this.surveyCache[suerveyId]);

        let idx = 0;
        
        var survey: Survey = {
            surveyId: "yrdy",
            options: {
                size: 1000000,                
            },
            questions: [
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Какова ваша реакция на первый вопрос?',
                    policy: { kind: 'AFTER_FIRST' },
                    required: true,
                    options: [
                        {
                            id: '0',
                            text: 'запутанно и страшно',
                        },
                        {
                            id: '1',
                            text: 'решать можно, но тяжело',
                        },
                        {
                            id: '2',
                            text: 'могло быть полегче, но удалось решить',
                        },
                        {
                            id: '3',
                            text: 'справился(-лась) с вопросом без особых трудностей',
                        },
                        {
                            id: '4',
                            text: 'было скучно, хотелось поинтереснее',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Как вы оцениваете сложность этого вопроса?',
                    policy: { kind: 'AFTER_EACH' },                    
                    required: true,
                    options: [
                        {
                            id: '1',
                            text: 'слишком легко',
                        },
                        {
                            id: '2',
                            text: 'легко',
                        },
                        {
                            id: '3',
                            text: 'средне',
                        },
                        {
                            id: '4',
                            text: 'сложно',
                        },
                        {
                            id: '5',
                            text: 'слишком сложно',
                        },
                        {
                            id: 'contains_errors',
                            text: 'этот вопрос содержит ошибки',
                        },
                        {
                            id: 'unknown_tokens',
                            text: 'я не понял этот вопрос из-за странных конструкций',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Как вы оцениваете скорость решения вами этого вопроса?',
                    policy: { kind: 'AFTER_EACH' },                    
                    required: true,
                    options: [
                        {
                            id: '1',
                            text: 'очень быстро',
                        },
                        {
                            id: '2',
                            text: 'быстро',
                        },
                        {
                            id: '3',
                            text: 'средне',
                        },
                        {
                            id: '4',
                            text: 'долго',
                        },
                        {
                            id: '5',
                            text: 'очень долго',
                        },
                        {
                            id: 'too_boring',
                            text: 'не дорешал(-а), вопрос этот меня утомил',
                        },
                        {
                            id: 'system_error',
                            text: 'дорешать до конца невозможно (ошибка в системе)',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Было ли заметно возрастание / убывание сложности за последние 3-5 вопросов?',                    
                    policy: { kind: 'AFTER_SPECIFIC', numbers: [3,6,9,12,15,18,21] },                    
                    required: true,
                    options: [
                        {
                            id: '1',
                            text: 'быстрое убывание сложности',
                        },
                        {
                            id: '2',
                            text: 'медленное убывание сложности',
                        },
                        {
                            id: '3',
                            text: 'изменений не заметно',
                        },
                        {
                            id: '4',
                            text: 'медленное возрастание сложности',
                        },
                        {
                            id: '5',
                            text: 'быстрое возрастание сложности',
                        },
                        {
                            id: 'cant_evaluate',
                            text: 'трудно оценить',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Соответствует ли возрастание / убывание сложности вашим предшествующим действиям (возрастает, когда вы вы решаете без ошибок, и снижается, если ошибок много)?',
                    policy: { kind: 'AFTER_SPECIFIC', numbers: [3,6,9,12,15,18,21] },   
                    required: true,
                    options: [
                        {
                            id: 'yes_always',
                            text: 'да, сложность меняется соответственно',
                        },
                        {
                            id: 'yes_sometimes',
                            text: 'да, но не всегда',
                        },
                        {
                            id: 'no_zero_correlation',
                            text: 'нет, связь не заметна',
                        },
                        {
                            id: 'no_incorrect_strategy_decisions',
                            text: 'нет, иногда реакция системы неадекватна',
                        },
                        {
                            id: 'no_incorrect_strategy_work',
                            text: 'нет, иногда реакция системы неадекватна',
                        },
                        {
                            id: 'cant_evaluate',
                            text: 'изменение сложности трудно оценить',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Сколько вопросов было задано?',
                    policy: { kind: 'AFTER_LAST' },                    
                    required: true,
                    options: [
                        {
                            id: 'few',
                            text: 'мало',
                        },
                        {
                            id: 'enough',
                            text: 'достаточно',
                        },
                        {
                            id: 'many',
                            text: 'много',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Были ли повторяющиеся или сильно похожие вопросы?',
                    policy: { kind: 'AFTER_LAST' },                    
                    required: true,
                    options: [
                        {
                            id: 'not',
                            text: 'нет',
                        },
                        {
                            id: 'slightly',
                            text: 'да, незначительно',
                        },
                        {
                            id: 'many',
                            text: 'да, много',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Насколько быстро система нашла удобную вам сложность вопросов?',
                    policy: { kind: 'AFTER_LAST' },                    
                    required: true,
                    options: [
                        {
                            id: '1',
                            text: 'очень медленно',
                        },
                        {
                            id: '2',
                            text: 'медленно',
                        },
                        {
                            id: '3',
                            text: 'средне',
                        },
                        {
                            id: '4',
                            text: 'быстро',
                        },
                        {
                            id: '5',
                            text: 'сразу',
                        },
                        {
                            id: 'no_target_complexity',
                            text: 'желаемой сложности не было',
                        },
                        {
                            id: 'no_target_complexity_stablization',
                            text: 'сложность вопросов не стабилизировалась',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'single-choice',
                    text: 'Считаете ли вы достаточной полноту покрытия изучаемой темы вопросами?',
                    policy: { kind: 'AFTER_LAST' },                    
                    required: true,
                    options: [
                        {
                            id: '1',
                            text: 'нет, для покрытия темы вопросов слишком мало',
                        },
                        {
                            id: '2',
                            text: 'нет, не хватает некоторых видов вопросов',
                        },
                        {
                            id: '3',
                            text: 'да, вопросов разных видов хватает',
                        },
                        {
                            id: '4',
                            text: 'да, но некоторых вопросов слишком много',
                        },
                        {
                            id: '5',
                            text: 'да, но вопросов вообще слишком много ',
                        },
                    ]
                },
                {
                    id: ++idx,
                    type: 'open-ended',
                    text: 'Хотите добавить что-то ещё?',
                    policy: { kind: 'AFTER_LAST' },                    
                    required: false,
                },
            ],
        }
        return E.right(survey);


        /*
        var result = await ajaxGet(`/survey/${suerveyId}`, TSurvey);
        if (E.isRight(result))
            this.surveyCache[suerveyId] = result.right;
        return result;*/
    }

    async postSurveyAnswer(surveyQuestionId: number, questionId: number, answer: string) : PromiseEither<RequestError, void> {
        //return ajaxPost(`/survey`, { surveyQuestionId, questionId, answer });

        return E.right(undefined);
    }

    async getCurrentUserAttemptSurveyVotes(surveyId: string, attemptId: number) : PromiseEither<RequestError, SurveyResultItem[]> {
        //return ajaxGet(`/survey/${encodeURIComponent(surveyId)}/user-votes?attemptId=${attemptId}`, io.array(TSurveyResultItem))
        return E.right([]);
    }
}
