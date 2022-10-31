import * as io from 'io-ts'

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

export type ExerciseCard = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    complexity: number,
    numberOfQuestions: number,
}

export const TExerciseCard: io.Type<ExerciseCard> = io.type({
    id: io.number,
    name: io.string,
    domainId: io.string,
    strategyId: io.string,
    backendId: io.string,
    complexity: io.number,
    numberOfQuestions: io.number,
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
}
export const TDomainConcept : io.Type<DomainConcept> = io.type({
    name: io.string,
    displayName: io.string,
    bitflags: io.number,
})

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

