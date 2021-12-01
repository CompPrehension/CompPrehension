import { action, flow, makeObservable, observable, runInAction, toJS } from "mobx";
import { inject, injectable } from "tsyringe";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import { Feedback } from "../types/feedback";
import { Question } from "../types/question";
import * as E from "fp-ts/lib/Either";
import { Interaction } from "../types/interaction";
import { SupplementaryQuestionRequest } from "../types/supplementary-question-request";
import * as NEArray from 'fp-ts/lib/NonEmptyArray'
import { pipe } from "fp-ts/lib/function";
import * as O from 'fp-ts/lib/Option'
import { Answer } from "../types/answer";
import { RequestError } from "../types/request-error";

/**
 * Store question data
 */
@injectable()
export class QuestionStore {
    @observable isQuestionLoading?: boolean = false;    
    @observable isFeedbackLoading: boolean = false;
    @observable isFeedbackVisible: boolean = true;
    @observable isQuestionFreezed: boolean = false;
    @observable feedback?: Feedback = undefined;
    @observable question?: Question = undefined;
    @observable lastAnswer: ReadonlyArray<Answer> = [];
    @observable answersHistory: Array<ReadonlyArray<Answer>> = [];
    @observable storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError, } = { tag: 'VALID' };

    constructor(@inject(ExerciseController) private exerciseController: IExerciseController) {
        makeObservable(this);        
    }

    private onQuestionLoaded = (question: Question) => {        
        // add question id to answers
        if (question.options.requireContext) {
            // regex searchs all tags with id='answer_id' and prepends them with question id
            question.text = question.text.replaceAll(/(\<.*?\sid\s*?\=([\'\"]))\s*(answer_.+?\2)(.*?\>)/igm, `$1question_${question.questionId}_$3$4`)
        }
        
        this.question = question;
        this.feedback = question.feedback ?? undefined;
        this.isFeedbackVisible = true;
        this.answersHistory = [];
        this.lastAnswer = question.responses ?? [];
    }

    @action
    private setValidStoreState = () => {
        if (this.storeState.tag !== 'VALID') {
            this.storeState = { tag: 'VALID' };
        }
    }

    @action
    private setErrorStoreState = (error: RequestError) => {        
        this.storeState = { tag: 'ERROR', error: error };
    }
    
    loadQuestion = flow(function* (this: QuestionStore, questionId: number) {
        this.setValidStoreState();

        this.isQuestionLoading = true;
        const dataEither: E.Either<RequestError, Question> = yield this.exerciseController.getQuestion(questionId);
        this.isQuestionLoading = false;

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }
        
        this.onQuestionLoaded(dataEither.right);
    })

    generateQuestion = flow(function* (this: QuestionStore, attemptId: number) {       
        this.setValidStoreState();
        
        this.isQuestionLoading = true;
        const dataEither: E.Either<RequestError, Question> = yield this.exerciseController.generateQuestion(attemptId);
        this.isQuestionLoading = false;

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }

        this.onQuestionLoaded(dataEither.right);
    })

    generateSupplementaryQuestion = flow(function* (this: QuestionStore, attemptId: number, questionId: number, violationLaws: string[]) {
        this.setValidStoreState();

        const questionRequest: SupplementaryQuestionRequest = {
            exerciseAttemptId: attemptId,
            questionId: questionId,
            violationLaws: pipe(
                NEArray.fromArray(violationLaws), 
                O.getOrElse(() => ["invalid_law"] as NEArray.NonEmptyArray<string>),
            ),
        };

        this.isQuestionLoading = true;
        const dataEither: E.Either<RequestError, Question | null | undefined | ''> = yield this.exerciseController.generateSupplementaryQuestion(questionRequest);
        this.isQuestionLoading = false;

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }

        if (dataEither.right) {
            this.onQuestionLoaded(dataEither.right);
        }
    })

    generateNextCorrectAnswer = flow(function* (this: QuestionStore) {
        const { question } = this;
        if (!question) {
            throw new Error("Current question not found");
        }

        this.setValidStoreState();
        
        this.isFeedbackLoading = true;
        const feedbackEither: E.Either<RequestError, Feedback> = yield this.exerciseController.generateNextCorrectAnswer(question.questionId);
        this.isFeedbackLoading = false;
        
        if (E.isLeft(feedbackEither)) {
            this.setErrorStoreState(feedbackEither.left);
            return;
        }

        const feedback = feedbackEither.right;        
        this.feedback = feedback;
        this.isFeedbackVisible = true;
        if (feedback && feedback.correctAnswers) {
            this.setFullAnswer(feedback.correctAnswers, false);                    
        }
    })

    private sendAnswersImpl = flow(function* (this: QuestionStore, attemptId: number, questionId: number, answers: readonly Answer[]) {
        const body: Interaction = toJS({
            attemptId,
            questionId,
            answers: toJS([...answers]),
        })

        this.setValidStoreState();

        this.isFeedbackLoading = true;
        const feedbackEither: E.Either<RequestError, Feedback> = yield this.exerciseController.addQuestionAnswer(body);
        this.isFeedbackLoading = false;
       
        if (E.isLeft(feedbackEither)) {
            this.setErrorStoreState(feedbackEither.left);
            return;
        }

        const feedback = feedbackEither.right;        
        this.feedback = feedback;
        this.isFeedbackVisible = true;
        if (feedback.correctAnswers) {
            this.setFullAnswer(feedback.correctAnswers, false);                  
        }
    });

    
    sendAnswers = flow(function* (this: QuestionStore) {
        const { question, lastAnswer } = this;      
        if (!question) {
            return;
        }
        yield this.sendAnswersImpl(question.attemptId, question.questionId, toJS(lastAnswer));        
    });


    onAnswersChanged = flow(function* (this: QuestionStore, answer: Answer[], sendAnswers: boolean = true) {
        this.answersHistory.push(answer);
        if (!sendAnswers) {
            return;
        }
        
        try {
            yield this.sendAnswers();
        } catch {            
            this.answersHistory.pop();
        }        
    })

    setFullAnswer = flow(function* (this: QuestionStore, fullAnswer: Answer[], sendAnswers: boolean = true) {
        if (!this.isAnswerChanged(fullAnswer)) {
            return false;
        }
        
        const prevLastAnswer = this.lastAnswer;
        this.lastAnswer = fullAnswer;
        if (prevLastAnswer.length > 0) {
            this.answersHistory.push(prevLastAnswer);
        }        
        
        if (!sendAnswers) { 
            return true;
        }

        try {
            yield this.sendAnswers();
            return true;
        } catch {
            // rollback asnwer if found unexpected error
            this.lastAnswer = prevLastAnswer;
            if (prevLastAnswer.length > 0) {
                this.answersHistory.pop();
            }
            return false;
        }
    })

    isAnswerChanged = (newAnswer: Answer[]): boolean => {
        const { lastAnswer, question } = this;
        if (!question) {
            throw new Error('no question');
        }

        const answersHistoryRaw = lastAnswer.map(x => x.answer);
        const newHistoryRaw = newAnswer.map(x => x.answer);

        switch(question.type) {
            case 'ORDER':
                // for ordering question type we must consider the order
                return newHistoryRaw.length !== answersHistoryRaw.length || JSON.stringify(newHistoryRaw) !== JSON.stringify(answersHistoryRaw);
            case 'MATCHING':
            case 'MULTI_CHOICE':
            case 'SINGLE_CHOICE':
                // for other questions we can ignore the order
                return newHistoryRaw.length !== answersHistoryRaw.length || JSON.stringify(newHistoryRaw.sort()) !== JSON.stringify(answersHistoryRaw.sort());
        }
    }
}
