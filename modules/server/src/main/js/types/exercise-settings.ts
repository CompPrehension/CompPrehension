import { NonEmptyArray } from 'fp-ts/lib/NonEmptyArray';
import * as io from 'io-ts'
import { ExerciseOptions, StrategySettingValues, TExerciseOptions, TStrategySettingValues } from './exercise-options';
import { nonEmptyArray } from './utils';

export type ExerciseListItem = {
    id: number,
    name: string,
    isPublic: boolean,
};
export const TExerciseListItem: io.Type<ExerciseListItem> = io.type({
    id: io.number,
    name: io.string,
    isPublic: io.boolean,
})

export type ExerciseListPermissions = {
    canCreateExercise: boolean,
    canImportInherit: boolean,
    canImportClone: boolean,
};
export const TExerciseListPermissions: io.Type<ExerciseListPermissions> = io.type({
    canCreateExercise: io.boolean,
    canImportInherit: io.boolean,
    canImportClone: io.boolean,
}, 'ExerciseListPermissions')

export const noExerciseListPermissions: ExerciseListPermissions = {
    canCreateExercise: false,
    canImportInherit: false,
    canImportClone: false,
};

export type ExerciseList = {
    exercises: ExerciseListItem[],
    permissions: ExerciseListPermissions,
};
export const TExerciseList: io.Type<ExerciseList> = io.type({
    exercises: io.array(TExerciseListItem),
    permissions: TExerciseListPermissions,
}, 'ExerciseList')

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

export type ExerciseCardSkill = {
    name: string,
    kind: ExerciseCardConceptKind,
}

export const TExerciseCardSkill: io.Type<ExerciseCardSkill> = io.type({
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
    skills: ExerciseCardSkill[]
}
export const TExerciseStage : io.Type<ExerciseStage> = io.type({
    numberOfQuestions: io.number,
    complexity: io.number,
    concepts: io.array(TExerciseCardConcept),
    laws: io.array(TExerciseCardLaw),
    skills: io.array(TExerciseCardSkill)
})


export type ExerciseCardPermissions = {
    canEdit: boolean,
    canDelete: boolean,
    canCloneToCourse: boolean,
    canCopyToGlobalPool: boolean,
    canUnlinkFromCourse: boolean,
}
export const TExerciseCardPermissions: io.Type<ExerciseCardPermissions> = io.type({
    canEdit: io.boolean,
    canDelete: io.boolean,
    canCloneToCourse: io.boolean,
    canCopyToGlobalPool: io.boolean,
    canUnlinkFromCourse: io.boolean,
}, 'ExerciseCardPermissions')

export type ExerciseCard = {
    id: number,
    name: string,
    domainId: string,
    strategyId: string,
    backendId: string,
    stages: NonEmptyArray<ExerciseStage>,
    tags: string[],
    options: ExerciseOptions,
    isPublic: boolean,
    permissions: ExerciseCardPermissions,
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
    isPublic: io.boolean,
    permissions: TExerciseCardPermissions,
})

export type DomainSkill = {
    name: string,
    displayName: string,
    childs: DomainSkill[]
}

export const TDomainSkill : io.Type<DomainSkill> = io.recursion('DomainSkill', () => io.type({
    name: io.string,
    displayName: io.string,
    childs: io.array(TDomainSkill),
}))


export type DomainLaw = {
    name: string,
    displayName: string,
    targetEnabled: boolean,
    childs: DomainLaw[],
}
export const TDomainLaw : io.Type<DomainLaw> = io.recursion('DomainLaw', () => io.type({
    name: io.string,
    displayName: io.string,
    targetEnabled: io.boolean,
    childs: io.array(TDomainLaw),
}))

export type DomainConcept = {
    name: string,
    displayName: string,
    targetEnabled: boolean,
    childs: DomainConcept[],
}
export const TDomainConcept : io.Type<DomainConcept> = io.recursion('DomainConcept', () => io.type({
    name: io.string,
    displayName: io.string,
    targetEnabled: io.boolean,
    childs: io.array(TDomainConcept),
}))

export type Domain = {
    id: string,
    displayName: string,
    description: string | null,
    laws: DomainLaw[],
    concepts: DomainConcept[],
    skills: DomainSkill[],
    tags: string[],
}
export const TDomain : io.Type<Domain> = io.type({
    id: io.string,
    displayName: io.string,
    description: io.union([io.string, io.null]),
    laws: io.array(TDomainLaw),
    skills: io.array(TDomainSkill),
    concepts: io.array(TDomainConcept),
    tags: io.array(io.string),
})

/** A field of the strategy settings form; its name is the key in the stored settings. */
export type StrategySettingField =
    | { kind: 'FLAG', name: string, label: string }
    | { kind: 'NUMERIC', name: string, label: string, min: number, max: number }
    | { kind: 'CHOICE', name: string, label: string, options: { value: string, label: string }[] }
    | { kind: 'GROUP', name: string, label: string, fields: StrategySettingField[] }
export const TStrategySettingField: io.Type<StrategySettingField> = io.recursion('StrategySettingField', () => io.union([
    io.type({ kind: io.literal('FLAG'), name: io.string, label: io.string }),
    io.type({ kind: io.literal('NUMERIC'), name: io.string, label: io.string, min: io.number, max: io.number }),
    io.type({ kind: io.literal('CHOICE'), name: io.string, label: io.string, options: io.array(io.type({ value: io.string, label: io.string })) }),
    io.type({ kind: io.literal('GROUP'), name: io.string, label: io.string, fields: io.array(TStrategySettingField) }),
]))

export type Strategy = {
    id: string,
    displayName: string,
    description: string | null,
    options: {
        multiStagesEnabled: boolean,
    },
    settings: {
        fields: StrategySettingField[],
        defaults: StrategySettingValues,
    },
}
export const TStrategy: io.Type<Strategy> = io.type({
    id: io.string,
    displayName: io.string,
    description: io.union([io.string, io.null]),
    options: io.type({
        multiStagesEnabled: io.boolean,
    }),
    settings: io.type({
        fields: io.array(TStrategySettingField),
        defaults: TStrategySettingValues,
    }),
})

export type QuestionBankSearchResult = {
    count: number,
    topRatedCount: number,
    questions: {
        metadataId: number,
        name: string,
    }[],
}
export const TQuestionBankSearchResult: io.Type<QuestionBankSearchResult> = io.type({
    count: io.number,
    topRatedCount: io.number,
    questions: io.array(io.type({
        metadataId: io.number,
        name: io.string,
    })),
})
