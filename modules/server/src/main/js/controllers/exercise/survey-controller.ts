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
        var result = await ajaxGet(`/api/survey/${suerveyId}`, TSurvey);
        if (E.isRight(result))
            this.surveyCache[suerveyId] = result.right;
        return result;
    }

    async postSurveyAnswer(surveyQuestionId: number, questionId: number, answer: string) : PromiseEither<RequestError, void> {
        return ajaxPost(`/api/survey`, { surveyQuestionId, questionId, answer });
    }

    async getCurrentUserAttemptSurveyVotes(surveyId: string, attemptId: number) : PromiseEither<RequestError, SurveyResultItem[]> {
        return ajaxGet(`/api/survey/${encodeURIComponent(surveyId)}/user-votes?attemptId=${attemptId}`, io.array(TSurveyResultItem))
    }
}
