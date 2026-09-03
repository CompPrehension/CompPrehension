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

/**
 * One question per display mode, so every renderer can be worked on without a backend.
 * The ids match the attempt handed out by the mocked `createExerciseAttempt`.
 */
export const mockQuestions: Record<number, Question> = {
    1: {
        type: 'SINGLE_CHOICE',
        questionId: 1,
        questionMetadataId: 1,
        text: 'question text',
        answers: [
            { id: 0, text: longAnswer },
            { id: 1, text: 'answer2' + longAnswer },
            { id: 2, text: 'answer3' + longAnswer },
        ],
        responses: [],
        feedback: null,
        options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'radio' },
    },
    2: {
        type: 'MULTI_CHOICE',
        questionId: 2,
        questionMetadataId: 2,
        text: 'question text',
        answers: [
            { id: 0, text: longAnswer },
            { id: 1, text: 'answer2' + longAnswer },
            { id: 2, text: 'answer3' + longAnswer },
        ],
        responses: [],
        feedback: null,
        options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'switch' },
    },
    3: {
        type: 'SINGLE_CHOICE',
        questionId: 3,
        questionMetadataId: 3,
        text: 'question text with <span id="answer_0">select1</span> and <span id="answer_1">select2</span>',
        answers: [],
        responses: [],
        feedback: null,
        options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'radio' },
    },
    4: {
        type: 'MULTI_CHOICE',
        questionId: 4,
        questionMetadataId: 4,
        text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>',
        answers: [],
        responses: [],
        feedback: null,
        options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'switch' },
    },
    5: {
        type: 'MATCHING',
        questionId: 5,
        questionMetadataId: 5,
        text: 'question text ',
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
    6: {
        type: 'MATCHING',
        questionId: 6,
        questionMetadataId: 6,
        text: 'question text with <span id="answer_0">drop</span> and <span id="answer_1">drop</span>',
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
    7: {
        type: 'MULTI_CHOICE',
        questionId: 7,
        questionMetadataId: 7,
        text: 'question text with <span id="answer_0"></span> and <span id="answer_1"></span>',
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
    8: {
        type: 'MATCHING',
        questionId: 8,
        questionMetadataId: 8,
        text: 'question text with <span id="answer_0">pick one</span> and <span id="answer_1">pick another</span>',
        answers: [],
        groups,
        responses: [],
        feedback: null,
        options: { requireContext: true, showSupplementaryQuestions: true, displayMode: 'combobox', multipleSelectionEnabled: false },
    },
    9: {
        type: 'MATCHING',
        questionId: 9,
        questionMetadataId: 9,
        text: 'question text',
        answers: [
            { id: 0, text: 'test1' },
            { id: 1, text: 'test2' },
        ],
        groups,
        responses: [],
        feedback: null,
        options: { requireContext: false, showSupplementaryQuestions: true, displayMode: 'combobox', multipleSelectionEnabled: false },
    },
};

export const mockAttempt = {
    attemptId: -1,
    exerciseId: -1,
    questionIds: Object.keys(mockQuestions).map(Number),
};
