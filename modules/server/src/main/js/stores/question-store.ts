import * as E from "fp-ts/lib/Either";
import { makeAutoObservable, toJS } from "mobx";
import { questionController } from "../controllers";
import { Answer } from "../types/answer";
import { Feedback } from "../types/feedback";
import { Interaction } from "../types/interaction";
import { Question } from "../types/question";
import { RequestError } from "../types/request-error";
import { isNullOrUndefined } from "../utils/helpers";
import { SupplementaryQuestionStore } from "./sup-question-store";

/**
 * Store question data
 */
export class QuestionStore {
    isFeedbackVisible: boolean = true;
    isQuestionFreezed: boolean = false;
    feedback?: Feedback = undefined;
    question?: Question = undefined;
    lastAnswer: ReadonlyArray<Answer> = [];
    answersHistory: Array<ReadonlyArray<Answer>> = [];
    supplementaryQuestion?: SupplementaryQuestionStore;
    questionState: 'INITIAL' | 'LOADING' | 'LOADED' | 'ANSWER_EVALUATING' | 'COMPLETED' = 'INITIAL';
    storeState: { tag: 'VALID' } | { tag: 'ERROR', error: RequestError, } = { tag: 'VALID' };

    constructor() {
        makeAutoObservable(this);
    }

    private onQuestionLoaded = (question: Question) => {
        // add question id to answers
        if (question.options.requireContext) {
            // regex searchs all tags with id='answer_id' and prepends them with question id
            var allMatches = question.text.matchAll(/(\<\w.*?\sid\s*?\=([\'\"]))\s*(answer_(\d+?))\2(.*?\>)/igm);
            [...allMatches].forEach((match, matchIdx) => {
                question.text = question.text.replace(
                    match[0],
                    `${match[1]}question_${question.questionId}_${match[3]}_${matchIdx}${match[2]} data-answer-id='${match[4]}' ${match[5]}`
                )
            })
        }

        this.question = question;
        this.supplementaryQuestion = new SupplementaryQuestionStore(question.questionId);
        this.feedback = question.feedback ?? undefined;
        this.isFeedbackVisible = true;
        this.answersHistory = [];
        this.lastAnswer = question.responses ?? [];

        if (question.feedback && question.feedback.stepsLeft === 0) {
            this.setQuestionState('COMPLETED');
        }
    }

    private onAnswerEvaluated(feedback: Feedback) {
        this.feedback = feedback;
        this.isFeedbackVisible = true;
        if (feedback && feedback.correctAnswers) {
            this.setFullAnswer(feedback.correctAnswers, false);
            if (!isNullOrUndefined(feedback.stepsLeft) && feedback.stepsLeft === 0) {
                this.setQuestionState('COMPLETED');
            }
        }
    }

    setQuestionState = (newState: QuestionStore['questionState']) => {
        if (this.questionState !== newState)
            this.questionState = newState;
    }

    private setValidStoreState = () => {
        if (this.storeState.tag !== 'VALID') {
            this.storeState = { tag: 'VALID' };
        }
    }

    private setErrorStoreState = (error: RequestError) => {
        this.storeState = { tag: 'ERROR', error: error };
    }

    loadQuestion = async (questionId: number) => {
        this.setValidStoreState();

        this.setQuestionState('LOADING');
        const dataEither = await questionController.getQuestion(questionId);
        this.setQuestionState('LOADED');

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }

        this.onQuestionLoaded(dataEither.right);
    }

    generateQuestion = async (attemptId: number) => {
        this.setValidStoreState();

        this.setQuestionState('LOADING');
        const dataEither = await questionController.generateQuestionByAttempt(attemptId);
        this.setQuestionState('LOADED');

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }

        this.onQuestionLoaded(dataEither.right);
    }

    generateQuestionByMetadata = async (metadataId: number) => {
        this.setValidStoreState();

        this.setQuestionState('LOADING');
        const dataEither = await questionController.generateQuestionByMetadata(metadataId);
        this.setQuestionState('LOADED');

        if (E.isLeft(dataEither)) {
            this.setErrorStoreState(dataEither.left);
            return;
        }

        this.onQuestionLoaded(dataEither.right);
    }

    generateNextCorrectAnswer = async () => {
        const { question } = this;
        if (!question) {
            throw new Error("Current question not found");
        }

        this.setValidStoreState();

        this.setQuestionState('ANSWER_EVALUATING');
        const feedbackEither = await questionController.generateNextCorrectAnswer(question.questionId);
        this.setQuestionState('LOADED');

        if (E.isLeft(feedbackEither)) {
            this.setErrorStoreState(feedbackEither.left);
            return;
        }

        this.onAnswerEvaluated(feedbackEither.right);
    }

    private sendAnswersImpl = async (questionId: number, answers: readonly Answer[]) => {
        const body: Interaction = toJS({
            questionId,
            answers: toJS([...answers]),
        })

        this.setValidStoreState();

        this.setQuestionState('ANSWER_EVALUATING');
        const feedbackEither = await questionController.addQuestionAnswer(body);
        this.setQuestionState('LOADED');

        if (E.isLeft(feedbackEither)) {
            this.setErrorStoreState(feedbackEither.left);
            return;
        }

        this.onAnswerEvaluated(feedbackEither.right);
    }

    sendAnswers = async () => {
        const { question, lastAnswer } = this;
        if (!question) {
            return;
        }
        await this.sendAnswersImpl(question.questionId, toJS(lastAnswer));
    }

    onAnswersChanged = async (answer: Answer[], sendAnswers: boolean = true) => {
        this.answersHistory.push(answer);
        if (!sendAnswers) {
            return;
        }

        try {
            await this.sendAnswers();
        } catch {
            this.answersHistory.pop();
        }
    }

    setFullAnswer = async (fullAnswer: Answer[], sendAnswers: boolean = true) => {
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
            await this.sendAnswers();
            return true;
        } catch {
            // rollback asnwer if found unexpected error
            this.lastAnswer = prevLastAnswer;
            if (prevLastAnswer.length > 0) {
                this.answersHistory.pop();
            }
            return false;
        }
    }

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
