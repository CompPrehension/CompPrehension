import { injectable } from "tsyringe";
import { Survey, TSurvey } from "../../types/survey";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";
import * as E from "fp-ts/lib/Either";
import { RequestError } from "../../types/request-error";


@injectable()
export class SurveyController {


    async getSurvey(suerveyId: string): PromiseEither<RequestError, Survey> {

        return ajaxGet(`/survey/${suerveyId}`, TSurvey);

        return E.right({
            surveyId: '1',            
            questions: [{
                id: 1,
                type: 'yes-no',
                text: "Как Вы думаете, был ли вопрос сгенерирован человеком или машиной?",
                options: {
                    yesText: 'машина',
                    yesValue: '1',
                    noText: 'человек',
                    noValue: '0',
                },
            }],
        });
    }

    async postSurveyAnswer(surveyQuestionId: number, questionId: number, answer: string) : PromiseEither<RequestError, void> {
        return ajaxPost(`/survey`, { surveyQuestionId, questionId, answer });
    }
}