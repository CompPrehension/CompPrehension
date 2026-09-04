import { Answer } from '../types/answer';
import { FeedbackMessage } from '../types/feedback';
import { Question } from '../types/question';

const longAnswer = 'answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 answer1 answer1 answer1answer1answer1answer1answer1 answer1answer1 ';
const dragStyles = {
    dropzoneStyle: '{ "display": "inline-block", "minHeight": "40px", "minWidth": "80px" }',
    dropzoneHtml: 'drop',
    draggableStyle: '{ "padding": "10px", "border": "5px solid", "borderRadius": "5px", "borderColor": "black", "backgroundColor": "white" }',
};
const groups = [
    { id: 0, text: '<div style="width:70px; height: 40px;">group1<div/>' },
    { id: 1, text: '<div style="width:50px;height: 100px;">group2 group2 group2 group2<div/>' },
];

export type MockQuestion = {
    question: Question,
    correctAnswers: [number, number][],
};

export const mockQuestions: Record<number, MockQuestion> = {
    1: {
        question: {
            type: 'SINGLE_CHOICE',
            questionId: 1,
            questionMetadataId: 1,
            text: 'question text. Choose answer 1.',
            answers: [
                { id: 0, text: longAnswer },
                { id: 1, text: 'answer2' + longAnswer },
                { id: 2, text: 'answer3' + longAnswer },
            ],
            responses: [],
            feedback: null,
            options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'radio' },
        },
        correctAnswers: [[1, 1]],
    },
    2: {
        question: {
            type: 'MULTI_CHOICE',
            questionId: 2,
            questionMetadataId: 2,
            text: 'question text. Switch answers 0 and 2 to yes, answer 1 to no.',
            answers: [
                { id: 0, text: longAnswer },
                { id: 1, text: 'answer2' + longAnswer },
                { id: 2, text: 'answer3' + longAnswer },
            ],
            responses: [],
            feedback: null,
            options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'switch' },
        },
        correctAnswers: [[0, 1], [1, 0], [2, 1]],
    },
    3: {
        question: {
            type: 'SINGLE_CHOICE',
            questionId: 3,
            questionMetadataId: 3,
            text: 'question text with <span id="answer_0">select1</span> and <span id="answer_1">select2</span>. Choose select2.',
            answers: [],
            responses: [],
            feedback: null,
            options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'radio' },
        },
        correctAnswers: [[1, 1]],
    },
    4: {
        question: {
            type: 'MULTI_CHOICE',
            questionId: 4,
            questionMetadataId: 4,
            text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>. Switch the first answer to yes and the second one to no.',
            answers: [],
            responses: [],
            feedback: null,
            options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'switch' },
        },
        correctAnswers: [[0, 1], [1, 0]],
    },
    5: {
        question: {
            type: 'MATCHING',
            questionId: 5,
            questionMetadataId: 5,
            text: 'question text. Drag group1 onto test1 and test3, group2 onto test2.',
            answers: [
                { id: 0, text: 'test1' },
                { id: 1, text: 'test2' },
                { id: 3, text: 'test3' },
            ],
            groups,
            responses: [],
            feedback: null,
            options: {
                requireContext: false,
                showSupplementaryQuestions: true,
                displayMode: 'dragNdrop',
                multipleSelectionEnabled: true,
                ...dragStyles,
            },
        },
        correctAnswers: [[0, 0], [1, 1], [3, 0]],
    },
    6: {
        question: {
            type: 'MATCHING',
            questionId: 6,
            questionMetadataId: 6,
            text: 'question text with <span id="answer_0">drop</span> and <span id="answer_1">drop</span>. Drop group2 into the first slot and group1 into the second one.',
            answers: [],
            groups,
            responses: [],
            feedback: null,
            options: {
                requireContext: true,
                showSupplementaryQuestions: true,
                displayMode: 'dragNdrop',
                multipleSelectionEnabled: false,
                ...dragStyles,
            },
        },
        correctAnswers: [[0, 1], [1, 0]],
    },
    7: {
        question: {
            type: 'MULTI_CHOICE',
            questionId: 7,
            questionMetadataId: 7,
            text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>. Mark the first answer with the check and the second one with the cross.',
            answers: [],
            responses: [],
            feedback: null,
            options: {
                requireContext: true,
                showSupplementaryQuestions: true,
                displayMode: 'dragNdrop',
                dropzoneStyle: '{ "display": "inline-block", "height": "20px", "width": "20px" }',
                dropzoneHtml: '',
                draggableStyle: '{ "height": "20px", "width": "20px" }',
            },
        },
        correctAnswers: [[0, 0], [1, 1]],
    },
    8: {
        question: {
            type: 'MATCHING',
            questionId: 8,
            questionMetadataId: 8,
            text: 'question text with <span id="answer_0">pick one</span> and <span id="answer_1">pick another</span>. Choose group2 for pick one and group1 for pick another.',
            answers: [],
            groups,
            responses: [],
            feedback: null,
            options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'combobox', multipleSelectionEnabled: false },
        },
        correctAnswers: [[0, 1], [1, 0]],
    },
    9: {
        question: {
            type: 'MATCHING',
            questionId: 9,
            questionMetadataId: 9,
            text: 'question text. Choose group1 for test1 and group2 for test2.',
            answers: [
                { id: 0, text: 'test1' },
                { id: 1, text: 'test2' },
            ],
            groups,
            responses: [],
            feedback: null,
            options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'combobox', multipleSelectionEnabled: false },
        },
        correctAnswers: [[0, 0], [1, 1]],
    },
};

export const mockAttempt = {
    attemptId: -1,
    exerciseId: -1,
    questionIds: Object.keys(mockQuestions).map(Number),
};

const key = (pair: readonly number[]) => pair.join(':');

const pickRandom = <T,>(items: T[]): T => items[Math.floor(Math.random() * items.length)];

function currentAnswers(questionId: number): Answer[] {
    return mockQuestions[questionId]?.question.responses ?? [];
}

export function recordAnswers(questionId: number, answers: Answer[]) {
    const fixture = mockQuestions[questionId];
    if (fixture) {
        fixture.question.responses = answers;
    }
}

export function resetAnswers(questionId: number) {
    recordAnswers(questionId, []);
}

export type Grade = {
    isCorrect: boolean,
    grade: number,
    correctAnswers: Answer[],
    correctSteps: number,
    stepsWithErrors: number,
    stepsLeft: number,
    messages: FeedbackMessage[],
};

export function gradeAnswers(questionId: number, submitted: Answer[]): Grade {
    const expected = mockQuestions[questionId]?.correctAnswers ?? [];
    const isRight = (a: Answer) => expected.some(pair => key(pair) === key(a.answer));

    const correctAnswers = submitted.filter(isRight);
    const settled = new Set(correctAnswers.map(a => a.answer[0]));
    const previous = new Set(currentAnswers(questionId).map(a => key(a.answer)));
    const taken = submitted.filter(a => !previous.has(key(a.answer)));
    const wrong = taken.filter(a => !isRight(a));

    return {
        isCorrect: wrong.length === 0,
        grade: expected.length === 0 ? 1 : correctAnswers.length / expected.length,
        correctAnswers,
        correctSteps: correctAnswers.length,
        stepsWithErrors: submitted.length - correctAnswers.length,
        stepsLeft: expected.filter(pair => !settled.has(pair[0])).length,
        messages: wrong.map(a => ({
            type: 'ERROR',
            message: `${key(a.answer)} is not one of the expected pairs`,
            violationLaws: [{ name: 'mocked_law', canCreateSupplementaryQuestion: true }],
        })),
    };
}

export function nextCorrectAnswer(questionId: number): Answer[] {
    const current = currentAnswers(questionId);
    const remaining = (mockQuestions[questionId]?.correctAnswers ?? [])
        .filter(pair => !current.some(a => a.answer[0] === pair[0]));
    if (remaining.length === 0) {
        return current;
    }

    return [...current, { answer: pickRandom(remaining), isCreatedByUser: false }];
}
