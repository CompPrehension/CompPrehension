import { Domain, ExerciseCard, Strategy } from '../types/exercise-settings';

export const mockDomains: Domain[] = [
    {
        id: 'ProgrammingLanguageExpressionDomain',
        displayName: 'Expressions',
        description: 'Order of evaluation in expressions',
        tags: ['C++', 'basics'],
        laws: [
            {
                name: 'operator_precedence',
                displayName: 'Operator precedence',
                bitflags: 1,
                childs: [
                    { name: 'operator_binary_precedence', displayName: 'Binary operators', bitflags: 2, childs: [] },
                ],
            },
            { name: 'operator_associativity', displayName: 'Operator associativity', bitflags: 4, childs: [] },
        ],
        concepts: [
            {
                name: 'operator',
                displayName: 'Operator',
                bitflags: 1,
                childs: [
                    { name: 'operator_binary_+', displayName: 'Binary +', bitflags: 2, childs: [] },
                    { name: 'operator_binary_*', displayName: 'Binary *', bitflags: 4, childs: [] },
                ],
            },
            { name: 'precedence', displayName: 'Precedence', bitflags: 8, childs: [] },
        ],
        skills: [
            {
                name: 'evaluate_expression',
                displayName: 'Evaluate an expression',
                childs: [
                    { name: 'find_next_operator', displayName: 'Find the next operator', childs: [] },
                ],
            },
        ],
    },
];

export const mockStrategies: Strategy[] = [
    {
        id: 'StaticStrategy',
        displayName: 'Static strategy',
        description: 'Hands out questions in a fixed order',
        options: { multiStagesEnabled: true },
    },
    {
        id: 'GradeConfidenceBaseStrategy',
        displayName: 'Grade confidence strategy',
        description: null,
        options: { multiStagesEnabled: false },
    },
];

export const mockBackends = ['JenaBackend', 'PelletBackend'];

const initialCard: ExerciseCard = {
    id: 1,
    name: 'Mocked exercise',
    domainId: mockDomains[0].id,
    strategyId: mockStrategies[0].id,
    backendId: mockBackends[0],
    isPublic: false,
    tags: ['C++'],
    options: {
        forceNewAttemptCreationEnabled: false,
        newQuestionGenerationEnabled: true,
        supplementaryQuestionsEnabled: true,
        correctAnswerGenerationEnabled: true,
        preferDecisionTreeBasedSupplementaryEnabled: false,
        debugButtonEnabled: true,
        maxExpectedConcurrentStudents: 7,
    },
    permissions: {
        canEdit: true,
        canDelete: true,
        canCloneToCourse: true,
        canCopyToGlobalPool: true,
        canUnlinkFromCourse: true,
    },
    stages: [
        {
            numberOfQuestions: 10,
            complexity: 0.5,
            concepts: [{ name: 'operator_binary_+', kind: 'TARGETED' }],
            laws: [{ name: 'operator_precedence', kind: 'TARGETED' }],
            skills: [{ name: 'find_next_operator', kind: 'PERMITTED' }],
        },
    ],
};

/**
 * Kept mutable so that saving the card on the settings page behaves like it does against
 * the real backend: the next load shows what was just saved.
 */
export let mockCard: ExerciseCard = structuredClone(initialCard);

export function saveMockCard(card: ExerciseCard) {
    mockCard = { ...mockCard, ...card };
}
