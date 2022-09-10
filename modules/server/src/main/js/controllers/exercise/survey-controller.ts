import { injectable } from "tsyringe";
import { Survey, TSurvey } from "../../types/survey";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";
import * as E from "fp-ts/lib/Either";
import { RequestError } from "../../types/request-error";


@injectable()
export class SurveyController {
    private surveyCache: Record<string, Survey> = {};
    async getSurvey(suerveyId: string): PromiseEither<RequestError, Survey> {
        if (this.surveyCache[suerveyId])
            return E.right(this.surveyCache[suerveyId]);

        var result = await ajaxGet(`/survey/${suerveyId}`, TSurvey);
        if (E.isRight(result))
            this.surveyCache[suerveyId] = result.right;
        return result;
    }

    async postSurveyAnswer(surveyQuestionId: number, questionId: number, answer: string) : PromiseEither<RequestError, void> {
        return ajaxPost(`/survey`, { surveyQuestionId, questionId, answer });
    }
}
