import { Answer } from '../types/answer';
import { Clarification, FeedbackMessage } from '../types/feedback';
import { MatchingQuestion, Question } from '../types/question';

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

/** A clarifying question as the backend keeps it: every reason comes with the explanation of its misconception. */
export type MockClarification = {
    prompt: string,
    options: { hypothesis: string, reason: string, explanation: string }[],
};

export type MockQuestion = {
    question: Question,
    correctAnswers: [number, number][],
    // keyed by an answer pair that several hypotheses explain; a correct pair is clarified as the strategy "always" does
    clarifications?: Record<string, MockClarification>,
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
    10: {
        question: {
            type: 'MATCHING',
            questionId: 10,
            questionMetadataId: 10,
            text: "<style>/* Выражение с дугами над частями (домен типов выражений). --depth у выражения и --lvl у части — высота над самыми\n   внутренними частями; все высоты в px, чтобы дуги и подписи с разными размерами шрифта считались от одной линейки. */\n.comp-ph-typed-expr {\n    --base: 7px;\n    --step: 17px;\n\n    display: inline-block;\n    font-family: Consolas, Monaco, \"Cascadia Mono\", monospace;\n    font-size: 20px;\n    line-height: 1.3;\n    padding: calc(var(--base) + var(--depth, 1) * var(--step) + 12px) 14px 8px;\n    background: #fff;\n    border-radius: 6px;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-token {\n    margin-left: 5px;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-part {\n    position: relative;\n    white-space: nowrap;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-part::after {\n    content: \"\";\n    position: absolute;\n    left: 3px;\n    right: 0;\n    top: calc(-1 * (var(--base) + var(--lvl) * var(--step)));\n    height: calc(var(--lvl) * var(--step));\n    border: 1px solid #9aa0a6;\n    border-bottom: none;\n    border-radius: calc(12px + var(--lvl) * 10px) calc(12px + var(--lvl) * 10px) 0 0;\n    pointer-events: none;\n}\n\n/* Подпись сидит на линии своей дуги; шаг между дугами больше её высоты, поэтому соседние уровни не пересекаются. */\n.comp-ph-typed-expr .comp-ph-expr-slot {\n    position: absolute;\n    left: 50%;\n    top: calc(-1 * (var(--base) + var(--lvl) * var(--step)));\n    transform: translate(-50%, -50%);\n    z-index: 5;\n    background: #fff;\n    padding: 0 3px;\n    font-size: 11px;\n    line-height: 13px;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-slot.show {\n    z-index: 20;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-slot > .dropdown-toggle {\n    font: inherit;\n    background: none;\n    border: none;\n    padding: 0 2px;\n    color: #0d6efd;\n    text-decoration: underline;\n    text-underline-offset: 2px;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-slot > .dropdown-toggle:disabled {\n    opacity: 0.45;\n    text-decoration: none;\n    cursor: not-allowed;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-part.solved > .comp-ph-expr-slot > .dropdown-toggle {\n    color: #198754;\n    font-weight: 600;\n    text-decoration: none;\n    opacity: 1;\n    cursor: default;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-part.solved > .comp-ph-expr-slot > .dropdown-toggle::after {\n    display: none;\n}\n\n.comp-ph-typed-expr .comp-ph-expr-part:has(> .comp-ph-expr-slot:hover) > .comp-ph-expr-token {\n    background: #fff3cd;\n    border-radius: 3px;\n}\n\n.comp-ph-typed-expr .dropdown-menu {\n    font-family: inherit;\n    font-size: 14px;\n    min-width: 8rem;\n}\n\n/* Код программы, подсвеченный на стороне генератора (классы Pygments). */\n.comp-ph-code pre {\n    background: #f6f7f9;\n    border-radius: 6px;\n    padding: 12px 14px;\n    font-size: 15px;\n}\n\n.comp-ph-code .k, .comp-ph-code .kn, .comp-ph-code .kc, .comp-ph-code .ow { color: #a626a4; }\n.comp-ph-code .s, .comp-ph-code .s1, .comp-ph-code .s2 { color: #50a14f; }\n.comp-ph-code .mi, .comp-ph-code .mf { color: #986801; }\n.comp-ph-code .nb, .comp-ph-code .nf { color: #4078f2; }\n\n.comp-ph-code .c, .comp-ph-code .c1 {\n    color: #8a8f98;\n    font-style: italic;\n}\n</style><p>The program has stopped at the marked place. Choose the result type of every part of the expression, starting from the innermost ones: click the label above a part. If a part causes an error, choose the error.</p><div class=\"comp-ph-code\"><pre><code><span class=\"n\">student</span> <span class=\"o\">=</span> <span class=\"p\">{</span><span class=\"s2\">&quot;grades&quot;</span><span class=\"p\">:</span> <span class=\"p\">[</span><span class=\"mi\">5</span><span class=\"p\">,</span> <span class=\"mi\">4</span><span class=\"p\">,</span> <span class=\"mi\">5</span><span class=\"p\">]}</span>\n<span class=\"c1\"># ... вы находитесь здесь ...</span></code></pre></div><div class=\"comp-ph-typed-expr\" style=\"--depth:2\"><span class=\"comp-ph-expr-part\" style=\"--lvl:2\"><span class=\"comp-ph-expr-slot\" id=\"answer_1\"></span><span class=\"comp-ph-expr-part\" style=\"--lvl:1\"><span class=\"comp-ph-expr-slot\" id=\"answer_0\"></span><span class=\"comp-ph-expr-token\">student</span><span class=\"comp-ph-expr-token\">[</span><span class=\"comp-ph-expr-token\">&quot;grades&quot;</span><span class=\"comp-ph-expr-token\">]</span></span><span class=\"comp-ph-expr-token\">[</span><span class=\"comp-ph-expr-token\">0</span><span class=\"comp-ph-expr-token\">]</span></span></div>",
            answers: [],
            groups: [{"id": 100, "text": "int"}, {"id": 101, "text": "str"}, {"id": 102, "text": "list[int]"}, {"id": 103, "text": "dict[str, list[int]]"}, {"id": 104, "text": "TypeError"}],
            responses: [],
            feedback: null,
            options: { requireContext: true, showSupplementaryQuestions: false, displayMode: 'inline', multipleSelectionEnabled: true },
        },
        correctAnswers: [[0, 102], [1, 100]],
        clarifications: {
            '1:100': {
                prompt: 'Why did you choose the type <code>int</code>?',
                options: [
                    {
                        hypothesis: 'rule',
                        reason: 'Indexing takes one element out of <code>list[int]</code>, and its elements have the type <code>int</code>.',
                        explanation: 'The expression <code>student["grades"][0]</code> has the type <code>int</code> because indexing takes one element out of <code>list[int]</code>, and its elements have the type <code>int</code>.',
                    },
                    {
                        hypothesis: 'index_type',
                        reason: 'Indexing gives the type of the index.',
                        explanation: 'The expression <code>student["grades"][0]</code> does have the type <code>int</code>, but the reasoning is wrong: an index only points to a position, and the result of indexing is the element of the sequence itself.',
                    },
                ],
            },
        },
    },
};

// total / len(grades): int for the division is explained both by "the result takes an operand's type" and by C-style division
mockQuestions[11] = {
    question: {
        ...(mockQuestions[10].question as MatchingQuestion),
        questionId: 11,
        questionMetadataId: 11,
        text: mockQuestions[10].question.text.split('<div class="comp-ph-code">')[0] + "<div class=\"comp-ph-code\"><pre><code><span class=\"n\">grades</span> <span class=\"o\">=</span> <span class=\"p\">[</span><span class=\"mi\">5</span><span class=\"p\">,</span> <span class=\"mi\">4</span><span class=\"p\">,</span> <span class=\"mi\">5</span><span class=\"p\">]</span>\n<span class=\"n\">total</span> <span class=\"o\">=</span> <span class=\"mi\">14</span>\n<span class=\"c1\"># ... вы находитесь здесь ...</span></code></pre></div><div class=\"comp-ph-typed-expr\" style=\"--depth:2\"><span class=\"comp-ph-expr-part\" style=\"--lvl:2\"><span class=\"comp-ph-expr-slot\" id=\"answer_1\"></span><span class=\"comp-ph-expr-token\">total</span><span class=\"comp-ph-expr-token\">/</span><span class=\"comp-ph-expr-part\" style=\"--lvl:1\"><span class=\"comp-ph-expr-slot\" id=\"answer_0\"></span><span class=\"comp-ph-expr-token\">len</span><span class=\"comp-ph-expr-token\">(</span><span class=\"comp-ph-expr-token\">grades</span><span class=\"comp-ph-expr-token\">)</span></span></span></div>",
        groups: [{ id: 100, text: 'int' }, { id: 101, text: 'float' }, { id: 102, text: 'str' }, { id: 103, text: 'list[int]' }, { id: 104, text: 'TypeError' }],
        responses: [],
    },
    correctAnswers: [[0, 100], [1, 101]],
    clarifications: {
        '1:100': {
            prompt: 'Why did you choose the type <code>int</code>?',
            options: [
                {
                    hypothesis: 'operand_type',
                    reason: 'The result takes the type of one of the operands.',
                    explanation: 'The expression <code>total / len(grades)</code> cannot have the type <code>int</code> because the operator <code>/</code> always returns a floating-point result.',
                },
                {
                    hypothesis: 'c_style_division',
                    reason: 'Dividing integers gives an integer.',
                    explanation: 'The expression <code>total / len(grades)</code> cannot have the type <code>int</code> because the operator <code>/</code> always returns a floating-point result.',
                },
            ],
        },
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
    clarification: Clarification | null,
    grade: number,
    correctAnswers: Answer[],
    correctSteps: number,
    stepsWithErrors: number,
    stepsLeft: number,
    messages: FeedbackMessage[] | null,
};

/** A hint is the system's answer, so like the backend it is never clarified. */
export function gradeAnswers(questionId: number, submitted: Answer[], isHint = false): Grade {
    const expected = mockQuestions[questionId]?.correctAnswers ?? [];
    const isRight = (a: Answer) => expected.some(pair => key(pair) === key(a.answer));

    const correctAnswers = submitted.filter(isRight);
    const settled = new Set(correctAnswers.map(a => a.answer[0]));
    const previous = new Set(currentAnswers(questionId).map(a => key(a.answer)));
    const taken = submitted.filter(a => !previous.has(key(a.answer)));
    const wrong = taken.filter(a => !isRight(a));

    const stepsLeft = expected.filter(pair => !settled.has(pair[0])).length;
    const clarification = isHint ? undefined : taken
        .map(a => mockQuestions[questionId]?.clarifications?.[key(a.answer)])
        .find(c => c !== undefined);
    pendingClarifications[questionId] = clarification;

    return {
        isCorrect: wrong.length === 0,
        clarification: clarification
            ? { prompt: clarification.prompt, options: clarification.options.map(({ hypothesis, reason }, id) => ({ id, hypothesis, reason })) }
            : null,
        grade: expected.length === 0 ? 1 : correctAnswers.length / expected.length,
        correctAnswers,
        correctSteps: correctAnswers.length,
        stepsWithErrors: submitted.length - correctAnswers.length,
        stepsLeft,
        // Like the backend: a right answer is confirmed, except the last one.
        messages: wrong.length === 0
            ? stepsLeft === 0 ? null : [{ type: 'SUCCESS', message: 'Correct, keep doing...', violationLaws: [] }]
            : wrong.map(a => ({
                type: 'ERROR',
                message: `${key(a.answer)} is not one of the expected pairs`,
                violationLaws: [{ name: 'mocked_law', canCreateSupplementaryQuestion: true }],
            })),
    };
}

const pendingClarifications: Record<number, MockClarification | undefined> = {};

/** Explanation of the misconception the student named; null for another reason. */
export function answerClarification(questionId: number, option: number | null): string | null {
    const clarification = pendingClarifications[questionId];
    pendingClarifications[questionId] = undefined;
    return option === null ? null : clarification?.options[option]?.explanation ?? null;
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
