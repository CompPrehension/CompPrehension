import { NonEmptyArray } from 'fp-ts/lib/NonEmptyArray';
import * as io from 'io-ts'
import { ExerciseOptions, TExerciseOptions } from './exercise-options';
import { nonEmptyArray } from './utils';

export type ExerciseListItem = {
    id: number,
    name: string,
    /*
    domainId: string,
    strategyId: string,
    backendId: string,*/
};
export const TExerciseListItem: io.Type<ExerciseListItem> = io.type({
    id: io.number,
    name: io.string,
    /*
    domainId: io.string,
    strategyId: io.string,
    backendId: io.string,*/
})

export type ExerciseCardConceptKind = 'FORBIDDEN' | 'PERMITTED' | 'TARGETED'
export type ExerciseCardConcept = {
    name: string,
    kind: ExerciseCardConceptKind,
}
export const TExerciseCardConcept: io.Type<ExerciseCardConcept> = io.type({
    name: io.string,
    kind: io.keyof({
        'FORBIDDEN': null,
        'PERMITTED': null,
        'TARGETED': null,
    })
})

export type ExerciseCardLaw = {
    name: string,
    kind: ExerciseCardConceptKind,
}
export const TExerciseCardLaw: io.Type<ExerciseCardLaw> = io.type({
    name: io.string,
    kind: io.keyof({
        'FORBIDDEN': null,
        'PERMITTED': null,
        'TARGETED': null,
    })
})


export type ExerciseStage = {
    numberOfQuestions: number,
    complexity: number,
    concepts: ExerciseCardConcept[],
    laws: ExerciseCardLaw[],
}
export const TExerciseStage : io.Type<ExerciseStage> = io.type({
    numberOfQuestions: io.number,
    complexity: io.number,
    concepts: io.array(TExerciseCardConcept),
    laws: io.array(TExerciseCardLaw),
})


export type ExerciseCard = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    stages: NonEmptyArray<ExerciseStage>,
    tags: string[],
    options: ExerciseOptions,
}

export const TExerciseCard: io.Type<ExerciseCard> = io.type({
    id: io.number,
    name: io.string,
    domainId: io.string,
    strategyId: io.string,
    backendId: io.string,
    stages: nonEmptyArray(TExerciseStage),    
    tags: io.array(io.string),
    options: TExerciseOptions,
})


export type DomainLaw = {
    name: string,
    displayName: string,
    bitflags: number,
    childs: DomainLaw[],
}
export const TDomainLaw : io.Type<DomainLaw> = io.recursion('DomainLaw', () => io.type({
    name: io.string,
    displayName: io.string,
    bitflags: io.number,
    childs: io.array(TDomainLaw),
}))

export enum DomainConceptFlag {
    VisibleToTeacher = 1,
    TargetEnabled = 1 << 1,
}

export type DomainConcept = {
    name: string,
    displayName: string,
    bitflags: number,
    childs: DomainConcept[],
}
export const TDomainConcept : io.Type<DomainConcept> = io.recursion('DomainConcept', () => io.type({
    name: io.string,
    displayName: io.string,
    bitflags: io.number,
    childs: io.array(TDomainConcept),
}))

export type Domain = {
    id: string,
    displayName: string,
    description: string | null,
    laws: DomainLaw[],
    concepts: DomainConcept[],
    tags: string[],
}
export const TDomain : io.Type<Domain> = io.type({
    id: io.string,
    displayName: io.string,
    description: io.union([io.string, io.null]),
    laws: io.array(TDomainLaw),
    concepts: io.array(TDomainConcept),
    tags: io.array(io.string),
})

export type Strategy = {
    id: string,
    displayName: string,
    description: string | null,
    options: {
        multiStagesEnabled: boolean,
    },
}
export const TStrategy: io.Type<Strategy> = io.type({
    id: io.string,
    displayName: io.string,
    description: io.union([io.string, io.null]),
    options: io.type({
        multiStagesEnabled: io.boolean,
    }),
})

export type QuestionBankCount = {
    count: number,
    topRatedCount: number,
}
export const TQuestionBankCount: io.Type<QuestionBankCount> = io.type({
    count: io.number,
    topRatedCount: io.number,
})

