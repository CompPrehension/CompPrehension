import { action, computed, makeObservable, observable, runInAction, toJS } from "mobx";
import { Question } from "../types/question";
import { Interaction } from "../types/interaction";
import { SupplementaryFeedback, SupplementaryQuestionRequest } from "../types/supplementary-question";
import { Answer } from "../types/answer";
import { NonEmptyArray } from "fp-ts/lib/NonEmptyArray";
import { IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { absurd } from "fp-ts/lib/function";

export class SupplementaryQuestionStore {
    @observable sourceQuestionId: number;
    @observable feedback?: SupplementaryFeedback = undefined;
    @observable question?: Question = undefined;
    @observable answer: ReadonlyArray<Answer> = [];
    @observable questionState: 'INITIAL' | 'LOADING' | 'LOADED' | 'ANSWER_EVALUATING' | 'COMPLETED' = 'INITIAL';

    constructor(private exerciseController: IExerciseController, sourceQuestionId: number) {
        this.sourceQuestionId = sourceQuestionId;
        
        makeObservable(this);
    }

    @action
    setQuestionState = (newState: SupplementaryQuestionStore['questionState']) => {
        if (this.questionState !== newState)
            this.questionState = newState;
    }

    @computed
    get isQuestionFreezed() {
        return this.questionState !== 'LOADED'
    }

    @computed
    get isFeedbackLoading() {
        return this.questionState === 'ANSWER_EVALUATING'
    }

    @computed
    get canSendQuestionAnswers() : boolean {
        if (!this.question)
            return false;

        switch (this.question.type) {
            case 'SINGLE_CHOICE':
            case 'MULTI_CHOICE':
                return this.answer.length > 0;
            case 'ORDER':
                return true;
            case 'MATCHING':
                return this.question.groups.length === this.question.answers.length;
            default:
                // compile-time checking whether the question has `never` type 
                // to ensure that all case branches have been processed
                return absurd<boolean>(this.question);
        }
    }

    @computed
    get questionSubmitMode() : 'IMPLICIT' | 'EXPLICIT' | null  {
        if (!this.question)
            return null;

        return this.question.type === 'SINGLE_CHOICE' ? 'IMPLICIT' : 'EXPLICIT';
    }

    @action
    generateSupplementaryQuestion = async (violationLaws: string[]) => {     
        if (violationLaws.length === 0)
            throw new Error("violationLaws mist be non-empty");

        this.setQuestionState('LOADING');
        const questionRequest: SupplementaryQuestionRequest = {
            questionId: this.sourceQuestionId,
            violationLaws: violationLaws as NonEmptyArray<string>,
        };        
        const dataEither = await this.exerciseController.generateSupplementaryQuestion(questionRequest);

        runInAction(() => {
            if (E.isLeft(dataEither)) {                
                this.setQuestionState('LOADED');
                return;
            }
            
            this.#onQuestionLoaded(dataEither.right.question, dataEither.right.message);
        })
    }

    @action
    sendAnswers = async () => {
        const { question } = this;
        if (!question)
            throw new Error("Question is empty");

        const body: Interaction = toJS({
            attemptId: question.attemptId,
            questionId: question.questionId,
            answers: toJS([...this.answer]),
        })

        this.setQuestionState('ANSWER_EVALUATING');
        const feedbackEither = await this.exerciseController.addSupplementaryQuestionAnswer(body);

        runInAction(() => { 
            if (E.isLeft(feedbackEither)) {
                this.setQuestionState('LOADED');
                return;
            }
    
            this.setQuestionState('COMPLETED');
            this.feedback = feedbackEither.right;
        })
    }

    @action
    setAnswer = (newAnswer: Answer[]) => {
        this.answer = newAnswer;
    }

    #onQuestionLoaded = (question?: Question | null, feedback?: SupplementaryFeedback | null) => {        
        // add question id to answers
        if (question?.options.requireContext) {
            // regex searchs all tags with id='answer_id' and prepends them with question id
            var allMatches = question.text.matchAll(/(\<\w.*?\sid\s*?\=([\'\"]))\s*(answer_(\d+?))\2(.*?\>)/igm);
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
