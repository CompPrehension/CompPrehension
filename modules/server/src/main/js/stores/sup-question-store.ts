import * as E from "fp-ts/lib/Either";
import { absurd } from "fp-ts/lib/function";
import { NonEmptyArray } from "fp-ts/lib/NonEmptyArray";
import { makeAutoObservable, toJS } from "mobx";
import { questionController } from "../controllers";
import { Answer } from "../types/answer";
import { Interaction } from "../types/interaction";
import { Question } from "../types/question";
import { SupplementaryFeedback, SupplementaryQuestionRequest } from "../types/supplementary-question";

export class SupplementaryQuestionStore {
    sourceQuestionId: number;
    feedback?: SupplementaryFeedback = undefined;
    question?: Question = undefined;
    answer: ReadonlyArray<Answer> = [];
    questionState: 'INITIAL' | 'LOADING' | 'LOADED' | 'ANSWER_EVALUATING' | 'COMPLETED' = 'INITIAL';

    constructor(sourceQuestionId: number) {
        this.sourceQuestionId = sourceQuestionId;

        makeAutoObservable(this);
    }

    setQuestionState = (newState: SupplementaryQuestionStore['questionState']) => {
        if (this.questionState !== newState)
            this.questionState = newState;
    }

    get isQuestionFreezed() {
        return this.questionState !== 'LOADED'
    }

    get isFeedbackLoading() {
        return this.questionState === 'ANSWER_EVALUATING'
    }

    get canSendQuestionAnswers() : boolean {
        if (!this.question || this.questionState === "COMPLETED")
            return false;

        switch (this.question.type) {
            case 'SINGLE_CHOICE':
            case 'MULTI_CHOICE':
                return this.answer.length > 0;
            case 'ORDER':
                return true;
            case 'MATCHING':
                return this.answer.length === this.question.answers.length;
            default:
                // compile-time checking whether the question has `never` type
                // to ensure that all case branches have been processed
                return absurd<boolean>(this.question);
        }
    }

    get questionSubmitMode() : 'IMPLICIT' | 'EXPLICIT' | null  {
        if (!this.question)
            return null;

        return this.question.type === 'SINGLE_CHOICE' ? 'IMPLICIT' : 'EXPLICIT';
    }

    generateSupplementaryQuestion = async (violationLaws: string[]) => {
        if (violationLaws.length === 0)
            throw new Error("violationLaws mist be non-empty");

        this.setQuestionState('LOADING');
        const questionRequest: SupplementaryQuestionRequest = {
            questionId: this.sourceQuestionId,
            violationLaws: violationLaws as NonEmptyArray<string>,
        };
        const dataEither = await questionController.generateSupplementaryQuestion(questionRequest);

        if (E.isLeft(dataEither)) {
            this.setQuestionState('LOADED');
            return;
        }

        this.#onQuestionLoaded(dataEither.right.question, dataEither.right.message);
    }

    sendAnswers = async () => {
        const { question } = this;
        if (!question)
            throw new Error("Question is empty");

        const body: Interaction = toJS({
            questionId: question.questionId,
            answers: toJS([...this.answer]),
        })

        this.setQuestionState('ANSWER_EVALUATING');
        const feedbackEither = await questionController.addSupplementaryQuestionAnswer(body);

        if (E.isLeft(feedbackEither)) {
            this.setQuestionState('LOADED');
            return;
        }

        this.setQuestionState('COMPLETED');
        this.feedback = feedbackEither.right;
    }

    setAnswer = (newAnswer: Answer[]) => {
        this.answer = newAnswer;
    }

    #onQuestionLoaded = (question?: Question | null, feedback?: SupplementaryFeedback | null) => {
        // add question id to answers
        if (question?.options.requireContext) {
            // regex searchs all tags with id='answer_id' and prepends them with question id
            const allMatches = question.text.matchAll(/(<\w.*?\sid\s*?=(['"]))\s*(answer_(\d+?))\2(.*?>)/igm);
            [...allMatches].forEach((match, matchIdx) => {
                question.text = question.text.replace(
                    match[0],
                    `${match[1]}question_${question.questionId}_${match[3]}_${matchIdx}${match[2]} data-answer-id='${match[4]}' ${match[5]}`
                )
            })
        }

        this.question      = question ?? undefined;
        this.feedback      = feedback ?? undefined;
        this.answer        = question?.responses ?? [];
        this.questionState = !question ? 'COMPLETED' : 'LOADED';
    }
}
