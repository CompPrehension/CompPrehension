import * as io from 'io-ts'
import { ExerciseOptions, TExerciseOptions } from './exercise-options';

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


export type ExerciseCard = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    complexity: number,
    answerLength: number,
    concepts: ExerciseCardConcept[],
    laws: ExerciseCardLaw[],
    options: ExerciseOptions,
}

export const TExerciseCard: io.Type<ExerciseCard> = io.type({
    id: io.number,
    name: io.string,
    domainId: io.string,
    strategyId: io.string,
    backendId: io.string,
    complexity: io.number,
    answerLength: io.number,
    concepts: io.array(TExerciseCardConcept),
    laws: io.array(TExerciseCardLaw),
    options: TExerciseOptions,
})


export type DomainLaw = {
    name: string,
}
export const TDomainLaw : io.Type<DomainLaw> = io.type({
    name: io.string,    
})

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
    name: string,
    laws: DomainLaw[],
    concepts: DomainConcept[],
}
export const TDomain : io.Type<Domain> = io.type({
    id: io.string,
    name: io.string,
    laws: io.array(TDomainLaw),
    concepts: io.array(TDomainConcept),
})

