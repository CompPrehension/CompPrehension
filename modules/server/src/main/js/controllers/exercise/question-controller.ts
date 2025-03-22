import { injectable } from "tsyringe";
import { API_URL } from "../../appconfig";
import { Feedback, TFeedback } from "../../types/feedback";
import { Interaction } from "../../types/interaction";
import { Question, TQuestion } from "../../types/question";
import { RequestError } from "../../types/request-error";
import { SupplementaryQuestionRequest, SupplementaryQuestion, SupplementaryFeedback, TSupplementaryQuestion, TSupplementaryFeedback } from "../../types/supplementary-question";
import { ajaxGet, ajaxPost, PromiseEither } from "../../utils/ajax";

export interface IQuestionController {
    generateQuestionByAttempt(attemptId: number): PromiseEither<RequestError, Question>;
    generateQuestionByMetadata(metadataId: number): PromiseEither<RequestError, Question>;
    getQuestion(questionId: number): PromiseEither<RequestError, Question>;
    generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, SupplementaryQuestion>;
    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback>;
    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> ;
    addSupplementaryQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, SupplementaryFeedback> ;
}

@injectable()
export class QuestionController implements IQuestionController {

    generateQuestionByAttempt(attemptId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${API_URL}/api/question/generate?attemptId=${attemptId}`, TQuestion); 
    }

    generateQuestionByMetadata(metadataId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${API_URL}/api/question/generateByMetadata?metadataId=${metadataId}`, TQuestion); 
    }

    getQuestion(questionId: number): PromiseEither<RequestError, Question> {
        return ajaxGet(`${API_URL}/api/question?questionId=${questionId}`, TQuestion);
    }

    generateSupplementaryQuestion(questionRequest: SupplementaryQuestionRequest): PromiseEither<RequestError, SupplementaryQuestion> {
        return ajaxPost(`${API_URL}/api/question/generateSupplementaryQuestion`, questionRequest, TSupplementaryQuestion);
    }

    generateNextCorrectAnswer(questionId: number): PromiseEither<RequestError, Feedback> {
        return ajaxGet(`${API_URL}/api/question/generateNextCorrectAnswer?questionId=${questionId}`, TFeedback);
    }

    addQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, Feedback> {
        return ajaxPost(`${API_URL}/api/question/addQuestionAnswer`, interaction, TFeedback);
    }
    
    addSupplementaryQuestionAnswer(interaction: Interaction): PromiseEither<RequestError, SupplementaryFeedback> {
        return ajaxPost(`${API_URL}/api/question/addSupplementaryQuestionAnswer`, interaction, TSupplementaryFeedback);
    }
}
