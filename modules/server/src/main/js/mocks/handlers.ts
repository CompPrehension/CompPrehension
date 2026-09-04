import { HttpResponse, delay, http } from 'msw';
import { ExerciseCard } from '../types/exercise-settings';
import { Interaction } from '../types/interaction';
import { mockBackends, mockCard, mockDomains, mockStrategies, saveMockCard } from './exercise-settings';
import { Grade, gradeAnswers, mockAttempt, mockQuestions, nextCorrectAnswer, recordAnswers, resetAnswers } from './questions';

/** Pretend the backend is thinking, so loading states are actually visible. */
const THINKING_MS = 800;

// stepsLeft counts steps of this question; FINISH would mean the whole attempt is over,
// which makes the exercise page latch to 'completed' for every other question
const feedback = (grade: Grade) => ({ ...grade, strategyDecision: 'CONTINUE' });

/** RFC 7807 ProblemDetail, the same shape `GlobalExceptionHandler` returns. */
const problem = (status: number, title: string, detail: string) =>
    HttpResponse.json({ type: 'about:blank', status, title, detail }, {
        status,
        headers: { 'content-type': 'application/problem+json' },
    });

export const handlers = [
    http.get('/api/users/whoami', () => HttpResponse.json({
        id: 999999,
        displayName: 'front user',
        email: 'test@mail.ru',
        language: 'EN',
        permissions: { canViewGlobalPool: true },
    })),

    http.post('/api/users/language', async ({ request }) => {
        const { language } = await request.json() as { language: string };
        return HttpResponse.text(language);
    }),

    http.get('/api/course/my', () => HttpResponse.json([
        { id: 1, name: 'Mocked course', educationResourceId: 1, educationResourceName: 'Mocked LMS' },
    ])),

    http.get('/api/exercise/list', () => HttpResponse.json({
        exercises: [{ id: 1, name: 'Mocked exercise', isPublic: false }],
        permissions: { canCreateExercise: true, canImportInherit: true, canImportClone: true },
    })),

    http.get('/api/course/memberships', () => HttpResponse.json([
        { id: 1, name: 'Mocked course', educationResourceId: 1, educationResourceName: 'Mocked LMS' },
    ])),

    http.post('/api/course/exercise/add', () => new HttpResponse(null, { status: 200 })),
    http.delete('/api/course/exercise/remove', () => new HttpResponse(null, { status: 200 })),

    // ---- exercise settings page ----

    http.get('/api/refTables/domains', () => HttpResponse.json(mockDomains)),
    http.get('/api/refTables/strategies', () => HttpResponse.json(mockStrategies)),
    http.get('/api/refTables/backends', () => HttpResponse.json(mockBackends)),
    http.get('/api/refTables/domainLaws', () => HttpResponse.json(mockDomains[0].laws.map(l => l.name))),
    http.get('/api/refTables/domainConcepts', () => HttpResponse.json(mockDomains[0].concepts.map(c => c.name))),

    http.get('/api/exercise', () => HttpResponse.json(mockCard)),

    http.post('/api/exercise', async ({ request }) => {
        saveMockCard(await request.json() as ExerciseCard);
        return new HttpResponse(null, { status: 200 });
    }),

    http.put('/api/exercise', () => HttpResponse.json(mockCard.id + 1)),
    http.post('/api/exercise/:id/clone', () => HttpResponse.json(mockCard.id + 1)),
    http.delete('/api/exercise', () => new HttpResponse(null, { status: 200 })),

    http.post('/api/question-bank/search', async () => {
        await delay(THINKING_MS);
        return HttpResponse.json({
            count: 42,
            topRatedCount: 12,
            questions: [
                { metadataId: 1, name: 'a + b * c' },
                { metadataId: 2, name: 'a * b + c / d' },
            ],
        });
    }),

    // ---- exercise attempt ----

    http.get('/api/exercise/shortInfo', () => HttpResponse.json({
        id: -1,
        options: {
            debugButtonEnabled: false,
            forceNewAttemptCreationEnabled: false,
            correctAnswerGenerationEnabled: true,
            newQuestionGenerationEnabled: true,
            supplementaryQuestionsEnabled: true,
            preferDecisionTreeBasedSupplementaryEnabled: false,
            maxExpectedConcurrentStudents: 7,
        },
    })),

    // no attempt in progress: the backend answers 200 with an empty body
    http.get('/api/exercise/getExistingExerciseAttempt', () => new HttpResponse(null, { status: 200 })),

    http.get('/api/exercise/createExerciseAttempt', async () => {
        await delay(THINKING_MS);
        return HttpResponse.json(mockAttempt);
    }),

    http.get('/api/exercise/createDebugExerciseAttempt', async () => {
        await delay(THINKING_MS);
        return HttpResponse.json(mockAttempt);
    }),

    http.get('/api/exercise/getExerciseAttempt', () => HttpResponse.json(mockAttempt)),

    http.get('/api/exercise/getExercises', () => HttpResponse.json([1])),

    http.get('/api/exercise/getExerciseStatistics', () => HttpResponse.json([
        {
            attemptId: -1,
            questionsCount: mockAttempt.questionIds.length,
            totalInteractionsCount: 12,
            totalInteractionsWithErrorsCount: 3,
            averageGrade: 0.75,
        },
    ])),

    http.get('/api/question', async ({ request }) => {
        await delay(THINKING_MS);
        const questionId = Number(new URL(request.url).searchParams.get('questionId'));
        const fixture = mockQuestions[questionId];
        if (!fixture) {
            return problem(404, 'Not Found', `No mocked question with id ${questionId}`);
        }
        resetAnswers(questionId);
        return HttpResponse.json(fixture.question);
    }),

    // the exercise hands out a fixed list of question ids, so generation only happens
    // when the ui asks for one more
    http.get('/api/question/generate', async () => {
        await delay(THINKING_MS);
        resetAnswers(1);
        return HttpResponse.json(mockQuestions[1].question);
    }),

    http.get('/api/question/generateByMetadata', async () => {
        await delay(THINKING_MS);
        resetAnswers(1);
        return HttpResponse.json(mockQuestions[1].question);
    }),

    http.get('/api/question/generateNextCorrectAnswer', async ({ request }) => {
        await delay(THINKING_MS);
        const questionId = Number(new URL(request.url).searchParams.get('questionId'));
        const answers = nextCorrectAnswer(questionId);
        const graded = gradeAnswers(questionId, answers);
        recordAnswers(questionId, answers);
        return HttpResponse.json(feedback(graded));
    }),

    http.post('/api/question/addQuestionAnswer', async ({ request }) => {
        await delay(THINKING_MS);
        const interaction = await request.json() as Interaction;
        const graded = gradeAnswers(interaction.questionId, interaction.answers);
        recordAnswers(interaction.questionId, interaction.answers);
        return HttpResponse.json(feedback(graded));
    }),

    http.post('/api/question/addSupplementaryQuestionAnswer', async () => {
        await delay(THINKING_MS);
        return HttpResponse.json({
            message: { type: 'SUCCESS', message: 'test', violationLaws: [] },
            action: 'CONTINUE_AUTO',
        });
    }),

    // everything the mocks do not cover answers like the real backend would for a gap in
    // the data, which keeps the ui on its error paths instead of hanging
    http.all('/api/*', ({ request }) =>
        problem(501, 'Not Implemented', `No mock handler for ${new URL(request.url).pathname}`)),
];
