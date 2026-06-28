import React, {useCallback, useEffect, useMemo, useState} from "react";
import {container} from "tsyringe";
import {
    Domain,
    DomainConcept,
    DomainConceptFlag,
    DomainLaw,
    DomainSkill,
    ExerciseCardConcept,
    ExerciseCardConceptKind,
    ExerciseCardLaw,
    ExerciseCardSkill,
    Strategy
} from "../types/exercise-settings";
import {ExerciseCardViewModel, ExerciseSettingsStore, ExerciseStageStore} from "../stores/exercise-settings-store";
import {observer} from "mobx-react";
import {ToggleSwitch} from "../components/common/toggle";
import {Button, Form} from "react-bootstrap";
import {Link} from "react-router-dom";
import {Loader} from "../components/common/loader";
import {useTranslation} from "react-i18next";
import {Header} from "../components/common/header";
import { API_URL } from "../appconfig";
import { useCurrentUser, useSession } from "../hooks/session-context";
import { useCourseId } from "../hooks/use-course-id";
import { ExerciseRowBadge } from "../components/exercise/exercise-row-badge";
import { DeleteGlobalExerciseModal } from "../components/exercise/delete-global-exercise-modal";
import { UserController } from "../controllers/exercise/user-controller";
import * as E from "fp-ts/lib/Either";
import { canEditExercises } from "../utils/roles";

export const ExerciseSettings = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseSettingsStore));
    const { t } = useTranslation();
    const user = useCurrentUser();
    const session = useSession();
    const courseId = useCourseId();
    const [contextRoles, setContextRoles] = useState<string[]>([]);
    const canEdit = canEditExercises(contextRoles);
    useEffect(() => {
        (async () => {
            await exerciseStore.loadExercises(courseId);

            const currentExercise = new URL(window.location.href).searchParams.get("exerciseId");
            if (currentExercise) {
                await exerciseStore.loadExercise(Number.parseInt(currentExercise));
            }
        })()
    }, [courseId]);
    useEffect(() => {
        (async () => {
            const res = await container.resolve(UserController).getCurrentUser(courseId);
            if (E.isRight(res)) {
                setContextRoles(res.right.roles);
            }
        })()
    }, [courseId]);

    const onNewExerciseClicked = useCallback(() => {
        (async () => {
            await exerciseStore.createNewExecise();
        })()
    }, [exerciseStore.exercises?.length]);

    const onLangClicked = useCallback(() => {
        const currentLang = user?.language;
        const newLang = currentLang === "RU" ? "EN" : "RU";
        session.changeLanguage(newLang);
    }, [session, user]);

    if (exerciseStore.exercisesLoadStatus === 'LOADING') {
        return <Loader />;
    }

    if (!user)
        return <Loader />;

    return (
        <div className="container-fluid">
            <div className="pt-1 pb-3">
                <Header text={t('exercisesettings_title')}
                        languageHint={t('language_header')}
                        language={user?.language ?? "EN"}
                        onLanguageClicked={onLangClicked}
                        userHint={t('signedin_as_header')}
                        user={user.displayName}
                        userHref={null}
                        logoutLabel={t('logout_header')} />
            </div>
            <div className="flex-xl-nowrap row">
                <div className="col-xl-3 col-md-3 col-12 d-flex flex-column">
                    {canEdit && <Button variant="primary" className="mb-3" onClick={onNewExerciseClicked}>Create new</Button>}
                    <ul className="list-group">
                        {exerciseStore.exercises?.map(e =>
                            <Link key={e.id}
                                className={`list-group-item ${e.id === exerciseStore.currentCard?.id && "active" || ""}`}
                                to={`?exerciseId=${e.id}${courseId != null ? `&courseId=${courseId}` : ''}`}
                                onClick={() => exerciseStore.loadExercise(e.id)}
                                title={e.name} >
                                {e.name.length > 22 ? `${e.name.substring(0, 22)}...` : e.name}
                            </Link>
                        )}
                    </ul>
                </div>
                <div className="col-xl-9 col-md-9 col-12">
                    <ExerciseCardElement
                        store={exerciseStore}
                        card={exerciseStore.currentCard}
                        domains={exerciseStore.domains ?? []}
                        backends={exerciseStore.backends ?? []}
                        strategies={exerciseStore.strategies ?? []}
                        canEdit={canEdit}
                    />
                </div>
            </div>
        </div>
    );
})


type ExerciseCardElementProps = {
    store: ExerciseSettingsStore,
    card?: ExerciseCardViewModel | null,
    domains: Domain[],
    backends: string[],
    strategies: Strategy[],
    canEdit: boolean,
}

const ExerciseCardElement = observer((props: ExerciseCardElementProps) => {
    const { card, domains, backends, strategies, store, canEdit } = props;
    const { t } = useTranslation();
    const user = useCurrentUser();
    const conceptFlagNames = useMemo(() => {
        return [t('exercisesettings_optDenied'), t('exercisesettings_optAllowed'), t('exercisesettings_optTarget')]
    }, [user?.language])

    if (store.exercisesLoadStatus === 'EXERCISELOADING')
        return <Loader delay={200} />;

    if (card == null)
        return (<div>No exercise selected</div>);

    const currentDomain = domains.find(z => z.id === card.domainId);
    const stageDomainLaws = currentDomain?.laws
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
    const stageDomainConcepts = currentDomain?.concepts
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
    const stageDomainSkills = currentDomain?.skills
    const cardLaws = card.stages[0].laws.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardLaw>);
    const cardConcepts = card.stages[0].concepts.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardConcept>);
    const sharedDomainLaws = currentDomain?.laws
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) === 0);
    const sharedDomainConcepts = currentDomain?.concepts
        .filter(c => (c.bitflags & DomainConceptFlag.TargetEnabled) === 0);
    const sharedDomainSkills : DomainSkill[] = []; // TODO: temporarily disabled due to missing flags in domain skills
    const currentStrategy = strategies.find(s => s.id === card.strategyId);
    const linkType = store.cardLinkType;
    const isInherited = linkType === 'inherited';

    return (
        <div>
            <ExerciseModeBar store={store} linkType={linkType} courseId={store.courseId} canEdit={canEdit} />
            <fieldset disabled={isInherited || !canEdit} style={(isInherited || !canEdit) ? { pointerEvents: 'none', opacity: 0.65 } : undefined}>
            <form className="exercise-settings-form">
                <div className="form-group">
                    <label className="font-weight-bold" htmlFor="exampleInputEmail1">{t('exercisesettings_name')}</label>
                    <Form.Control value={card.name} type="email" id="exampleInputEmail1" aria-describedby="emailHelp" placeholder="Enter email" onChange={e => store.setCardName(e.target.value)} />
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_domain')}</label>
                    <Form.Control as="select" id="domainId" aria-describedby="domainDescription" value={card.domainId} onChange={e => store.setCardDomain(e.target.value)} title={currentDomain?.displayName}>
                        {domains?.map(d => <option key={d.id} value={d.id} title={d.description ?? d.displayName}>{d.displayName}</option>)}
                    </Form.Control>
                    <small id="domainDescription" className="form-text text-muted">{currentDomain?.description ?? ""}</small>
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_strategy')}</label>
                    <Form.Control as="select" id="strategyId" aria-describedby="strategyDescription" value={card.strategyId} onChange={e => store.setCardStrategy(e.target.value)} title={currentStrategy?.displayName}>
                        {strategies?.map(d => <option key={d.id} value={d.id} title={d.description ?? d.displayName}>{d.displayName}</option>)}
                    </Form.Control>
                    <small id="strategyDescription" className="form-text text-muted">{currentStrategy?.description ?? ""}</small>
                </div>

                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_qopt')}</label>
                    <Form.Check type="checkbox"
                                id="forceNewAttemptCreationEnabled"
                                label={t('exercisesettings_qopt_forceAttCreation')}
                                checked={card.options.forceNewAttemptCreationEnabled}
                                onChange={x => store.setCardOption('forceNewAttemptCreationEnabled', x.target.checked)} />
                    <Form.Check type="checkbox"
                                id="correctAnswerGenerationEnabled"
                                label={t('exercisesettings_qopt_genCorAnsw')}
                                checked={card.options.correctAnswerGenerationEnabled}
                                onChange={x => store.setCardOption('correctAnswerGenerationEnabled', x.target.checked)} />
                    <Form.Check type="checkbox"
                                id="newQuestionGenerationEnabled"
                                label={t('exercisesettings_qopt_forceShowGenNextQ')}
                                checked={card.options.newQuestionGenerationEnabled}
                                onChange={x => store.setCardOption('newQuestionGenerationEnabled', x.target.checked)} />
                    <Form.Check type="checkbox"
                                id="supplementaryQuestionsEnabled"
                                label={t('exercisesettings_qopt_supQ')}
                                checked={card.options.supplementaryQuestionsEnabled}
                                onChange={x => store.setCardOption('supplementaryQuestionsEnabled', x.target.checked)} />
                    <Form.Check type="checkbox"
                                id="preferDecisionTreeBasedSupplementaryEnabled"
                                label={t('exercisesettings_qopt_preferDTsup')}
                                checked={card.options.preferDecisionTreeBasedSupplementaryEnabled}
                                onChange={x => store.setCardOption('preferDecisionTreeBasedSupplementaryEnabled', x.target.checked)} />
                    <Form.Check type="checkbox"
                                id="debugButtonEnabled"
                                label={t('exercisesettings_qopt_debugBtn')}
                                checked={card.options.debugButtonEnabled}
                                onChange={x => store.setCardOption('debugButtonEnabled', x.target.checked)} />
                </div>

                <div className="form-group">
                    <label className="font-weight-bold" htmlFor={`maxExpectedConcurrentStudents`}>{t('exercisesettings_max_concurrent_students')}</label>
                    <Form.Control type="number"
                        id={`maxExpectedConcurrentStudents`}
                        value={store.currentCard?.options.maxExpectedConcurrentStudents}
                        onChange={e => store.setCardOption('maxExpectedConcurrentStudents', +e.target.value)} />
                </div>

                <div className="form-group">
                    <label htmlFor="survOptions" className="font-weight-bold">{t('exercisesettings_survey')}</label>
                    <div className="input-group mb-3">
                        <div className="input-group-prepend">
                            <div className="input-group-text">
                                <input checked={card.options.surveyOptions?.enabled}
                                    type="checkbox"
                                    aria-label="Checkbox for following text input"
                                    onChange={x => store.setCardSurveyEnabled(x.target.checked)} />
                            </div>
                        </div>
                        <Form.Control id="survOptions"
                            type="text"
                            value={card.options.surveyOptions?.surveyId}
                            aria-label="Text input with checkbox"
                            disabled={!card.options.surveyOptions?.enabled}
                            onChange={x => store.setCardSurveyId(x.target.value)} />
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="exTagsValues" className="font-weight-bold">{t('exercisesettings_tags')}</label>
                    {currentDomain?.tags.map((t, i) =>
                        <Form.Check key={i}
                            type="checkbox"
                            id={`tag_checkbox_${t}`}
                            label={t}
                            value={t}
                            checked={card.tags.includes(t)}
                            onChange={x => x.target.checked
                                ? store.setCardTags([...new Set([...card.tags, x.target.value])])
                                : store.setCardTags([...card.tags.filter(z => z !== x.target.value)])} />)}
                </div>
                {sharedDomainConcepts?.length
                    && <div className="form-group">
                        <div>
                            <label className="font-weight-bold">{t('exercisesettings_commonConcepts')}</label>
                        </div>
                        <div className="row">
                            <div className="col-md-12">
                                <div className="list-group list-group-flush">
                                    <ExerciseConcepts                                        
                                        id="common_concepts"
                                        store={store}
                                        concepts={sharedDomainConcepts}
                                        cardConcepts={cardConcepts}
                                        onChange={(concept, conceptValue, parent) => {
                                            store.setCardCommonConceptValue(concept.name, conceptValue)
                                            if (!parent)
                                                concept.childs.forEach(c => store.setCardCommonConceptValue(c.name, conceptValue));
                                            else
                                                store.setCardCommonConceptValue(parent.name, 'PERMITTED');
                                        }} />
                                </div>
                            </div>
                        </div>
                    </div>
                    || null
                }
                {sharedDomainLaws?.length
                    && <div className="form-group">
                        <label className="font-weight-bold">{t('exercisesettings_commonLaws')}</label>
                        <div className="list-group list-group-flush">
                            <ExerciseLaws
                                id="common_laws"
                                store={store}
                                laws={sharedDomainLaws}
                                cardLaws={cardLaws}
                                onChange={(law, lawValue, parent) => {
                                    store.setCardCommonLawValue(law.name, lawValue)
                                    if (!parent)
                                        law.childs.forEach(c => store.setCardCommonLawValue(c.name, lawValue));
                                    else
                                        store.setCardCommonLawValue(parent.name, 'PERMITTED');
                                }}
                            />
                        </div>
                    </div>
                    || null
                }
                {sharedDomainSkills?.length
                    && <div className="form-group">
                        <label className="font-weight-bold">{t('exercisesettings_commonSkills')}</label>
                        <div className="list-group list-group-flush">
                            <ExerciseSkills
                                id="common_skills"
                                store={store}
                                skills={sharedDomainSkills}
                                cardSkills={cardLaws}
                                onChange={(skill, skillValue, parent) => {
                                    store.setCardCommonSkillValue(skill.name, skillValue)
                                    if (!parent)
                                        skill.childs.forEach(c => store.setCardCommonSkillValue(c.name, skillValue));
                                    else
                                        store.setCardCommonSkillValue(parent.name, 'PERMITTED');
                                }}
                            />
                        </div>
                    </div>
                    || null
                }
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_stages')}</label>
                    <div className="list-group list-group-flush">
                        {card.stages.map((stage, stageIdx, stages) => 
                            <ExerciseStage
                                key={stageIdx} 
                                store={store} 
                                stage={stage} 
                                stageIdx={stageIdx}
                                showDeleteBtn={stages.length > 1}
                                strategy={currentStrategy}
                                stageDomainConcepts={stageDomainConcepts}
                                stageDomainSkills={stageDomainSkills}
                                stageDomainLaws={stageDomainLaws} />)
                        }
                    </div>
                </div>
                {currentStrategy?.options.multiStagesEnabled
                    ? <div style={{marginTop: "-1rem"}}>
                        <Button variant="success" onClick={() => store.addStage()}>{t('exercisesettings_addStage')}</Button>
                      </div>
                    : null
                }
            </form >
            </fieldset>
            <div className="mt-5">
                {canEdit && !isInherited && <Button variant="primary" className="mr-2" onClick={() => store.saveCard()}>{t('exercisesettings_save')}</Button>}
                {canEdit && !isInherited && <Button variant="primary" className="mr-2" onClick={() => store.saveCard().then(() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ''}`, '_blank')?.focus()) }>{t('exercisesettings_saveNopen')}</Button>}
                <Button variant="primary" className="mr-2" onClick={() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ''}`, '_blank')?.focus()}>{t('exercisesettings_open')}</Button>
                {canEdit && !isInherited && currentStrategy?.options.multiStagesEnabled &&
                    <Button variant="primary" className="mr-2" onClick={() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}${store.courseId != null ? `&courseId=${store.courseId}` : ''}&debug`, '_blank')?.focus()}>{t('exercisesettings_genDebugAtt')}</Button>
                }
            </div>
            </div >


    );
})

type ExerciseModeBarProps = {
    store: ExerciseSettingsStore,
    linkType: 'global' | 'original' | 'inherited' | 'cloned',
    courseId: number | null,
    canEdit: boolean,
};

const ExerciseModeBar = observer(({ store, linkType, courseId, canEdit }: ExerciseModeBarProps) => {
    const { t } = useTranslation();
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [busy, setBusy] = useState(false);
    const card = store.currentCard;
    if (!card) return null;
    if (!canEdit) return <div className="mb-3"><ExerciseRowBadge linkType={linkType} /></div>;

    const onConvertToClone = async () => {
        if (courseId == null) return;
        setBusy(true);
        await store.cloneCurrentToCourse(courseId);
        setBusy(false);
    };

    const onUnlink = async () => {
        if (courseId == null) return;
        setBusy(true);
        await store.unlinkFromCourse(courseId);
        setBusy(false);
    };

    const onCopyToPool = async () => {
        setBusy(true);
        const newId = await store.copyCurrentToPool();
        setBusy(false);
        if (newId != null) {
            window.location.href = `/pages/exercise-settings?exerciseId=${newId}`;
        }
    };

    const onDeleteClick = () => {
        if (card.isPublic) {
            setShowDeleteModal(true);
        } else if (window.confirm(t('exerciseModeBar_confirmDelete'))) {
            store.deleteCurrentExercise();
        }
    };

    const onConfirmGlobalDelete = async () => {
        setShowDeleteModal(false);
        setBusy(true);
        await store.deleteCurrentExercise();
        setBusy(false);
    };

    return (
        <div className="mb-3">
            <div className="mb-2">
                <ExerciseRowBadge linkType={linkType} />
            </div>
            <div className="btn-toolbar" role="toolbar">
                <div className="btn-group btn-group-sm flex-wrap" role="group" style={{ gap: '0.25rem' }}>
                    {linkType === 'inherited' && (
                        <>
                            <Button variant="warning" disabled={busy} onClick={onConvertToClone}>
                                {t('exerciseModeBar_convertToClone')}
                            </Button>
                            <Button variant="outline-danger" disabled={busy} onClick={onUnlink}>
                                {t('exerciseModeBar_unlinkFromCourse')}
                            </Button>
                        </>
                    )}
                    {linkType === 'original' && (
                        <Button variant="info" disabled={busy} onClick={onCopyToPool}>
                            {t('exerciseModeBar_copyToPool')}
                        </Button>
                    )}
                    {linkType !== 'inherited' && (
                        <Button variant="danger" disabled={busy} onClick={onDeleteClick}>
                            {t('exerciseModeBar_deleteExercise')}
                        </Button>
                    )}
                </div>
            </div>
            {showDeleteModal && (
                <DeleteGlobalExerciseModal
                    exerciseId={card.id}
                    onConfirm={onConfirmGlobalDelete}
                    onCancel={() => setShowDeleteModal(false)} />
            )}
        </div>
    );
});


type ExerciseStageProps = {
    store: ExerciseSettingsStore,
    stage: ExerciseStageStore,    
    stageIdx: number,
    showDeleteBtn?: boolean,
    strategy?: Strategy,
    stageDomainConcepts?: DomainConcept[],
    stageDomainLaws?: DomainLaw[],
    stageDomainSkills?: DomainSkill[],
}
const ExerciseStage = observer((props: ExerciseStageProps) => {
    const { t } = useTranslation();
    const { store, stage, strategy, stageIdx, showDeleteBtn, stageDomainConcepts, stageDomainLaws, stageDomainSkills } = props;
    const card = store.currentCard;
    if (!card)
        throw new Error('card not set');
    const cardConcepts = stage.concepts.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardConcept>);
    const cardLaws = stage.laws.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardLaw>);
    const cardSkills = stage.skills.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardSkill>);

    return (
        <div className="card mb-3">
            <div className="card-body">
                <div className="form-group" style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <div>{t('exercisesettings_stageN', { stageNumber: stageIdx + 1 })}</div>                                            
                    <div>
                        <span>{t('exercisesettings_questionsInBank')}:&nbsp;</span>
                        {
                            stage.bankLoadingState === 'IN_PROGRESS' || stage.bankSearchResult === null
                                ? <Loader styleOverride={{ width: '1rem', height: '1rem' }} delay={0} />
                                : <span>{`${stage.bankSearchResult.count} (${stage.bankSearchResult?.topRatedCount})`}</span>
                        }
                    </div>
                </div>
                {strategy?.options.multiStagesEnabled
                    && <div className="form-group">
                            <label className="font-weight-bold" htmlFor={`numberOfQuestions_stage${stageIdx}`}>{t('exercisesettings_stageN_qnumber')}</label>
                            <Form.Control type="text"
                                id={`numberOfQuestions_stage${stageIdx}`}
                                value={stage.numberOfQuestions}
                                onChange={e => store.setCardStageNumberOfQuestions(stageIdx, e.target.value)} />
                        </div>
                    || null
                }
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_qcomplexity')}</label>
                    <div className="d-flex">
                        <input type="range"
                            className="form-control-range"
                            id={`complexity_stage${stageIdx}`}
                            value={(stage.complexity ?? 0.5) * 100}
                            onChange={e => store.setCardStageComplexity(stageIdx, e.target.value)} />
                        <div className="ml-2">{stage.complexity.toFixed(2)}</div>
                    </div>
                </div>
                {(stageDomainConcepts && stageDomainConcepts.length > 0) &&
                    <div className="form-group">
                        <label className="font-weight-bold">{t('exercisesettings_stageN_concepts')}</label>
                        <div className="list-group list-group-flush">
                            <ExerciseConcepts
                                id={`stage${stageIdx}_concepts`}
                                store={store}
                                concepts={stageDomainConcepts}
                                cardConcepts={cardConcepts}
                                onChange={(concept, conceptValue, parent) => {
                                    store.setCardStageConceptValue(stageIdx, concept.name, conceptValue)
                                    if (!parent)
                                        concept.childs.forEach(c => store.setCardStageConceptValue(stageIdx, c.name, conceptValue));
                                    else
                                        store.setCardStageConceptValue(stageIdx, parent.name, 'PERMITTED');
                                }} />
                        </div>
                    </div>
                    || null}
                {(stageDomainLaws && stageDomainLaws.length > 0)
                    &&
                    <div className="form-group">
                        <label className="font-weight-bold">{t('exercisesettings_stageN_laws')}</label>
                        <div className="list-group list-group-flush">
                            <ExerciseLaws
                                id={`stage${stageIdx}_laws`}
                                store={store}
                                laws={stageDomainLaws}
                                cardLaws={cardLaws}
                                onChange={(law, lawValue, parent) => {
                                    store.setCardStageLawValue(stageIdx, law.name, lawValue)
                                    if (!parent)
                                        law.childs.forEach(c => store.setCardStageLawValue(stageIdx, c.name, lawValue));
                                    else
                                        store.setCardStageLawValue(stageIdx, parent.name, 'PERMITTED');
                                }}
                            />
                        </div>
                    </div>
                    || null
                }
                {(stageDomainSkills && stageDomainSkills.length > 0)
                    &&
                    <div className="form-group">
                        <label className="font-weight-bold">{t('exercisesettings_stageN_skills')}</label>
                        <div className="list-group list-group-flush">
                            <ExerciseSkills
                                id={`stage${stageIdx}_skills`}
                                store={store}
                                skills={stageDomainSkills}
                                cardSkills={cardSkills}
                                onChange={(skill, skillValue, parent) => {
                                    store.setCardStageSkillValue(stageIdx, skill.name, skillValue)
                                    if (!parent)
                                        skill.childs.forEach(c => store.setCardStageSkillValue(stageIdx, c.name, skillValue));
                                    else
                                        store.setCardStageSkillValue(stageIdx, parent.name, 'PERMITTED');
                                }}
                            />
                        </div>
                    </div>
                    || null
                }
                
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_stageN_matchedQuestionExamples')}</label>
                    {
                        stage.bankLoadingState === 'IN_PROGRESS' 
                            && <Loader styleOverride={{ width: '1rem', height: '1rem' }} delay={0} />
                            || <div className="list-group">
                                {
                                    stage.bankSearchResult.questions.length === 0
                                    ? <div className="list-group-item">{t('exercisesettings_noQuestionsFound')}</div>
                                    : stage.bankSearchResult.questions.map((q, i) =>
                                        <div key={i} className="list-group-item">
                                            <a target="_blank" href={`${API_URL}/pages/question?metadataId=${q.metadataId}`}>{q.name}</a>
                                        </div>)
                                }
                               </div>
                    }
                </div>
                {showDeleteBtn &&
                    <div className="d-flex justify-content-end">
                        <Button variant="danger" onClick={() => store.removeStage(stageIdx)}>{t('exercisesettings_removeStage')}</Button>
                    </div>
                    || null
                }
            </div>
        </div>
    );
})

type ExerciseConceptsProps = {
    id: string | number,
    store: ExerciseSettingsStore,
    concepts: DomainConcept[],
    cardConcepts: Record<string, ExerciseCardConcept>,
    onChange: (concept: DomainConcept, conceptValue: ExerciseCardConceptKind, parent?: DomainConcept) => void,
}
const ExerciseConcepts = observer((props: ExerciseConceptsProps) => {
    const { id, store, concepts, cardConcepts, onChange } = props;
    const { t  } = useTranslation();
    const card = store.currentCard;
    if (!card)
        throw new Error('card not set');
    const user = useCurrentUser();
    
    const conceptFlagNames = useMemo(() => {
        return [t('exercisesettings_optDenied'), t('exercisesettings_optAllowed'), t('exercisesettings_optTarget')]
    }, [user?.language])
    

    return (
        <>
            {concepts
                .map((coreConcept, idx) =>
                    <div key={idx} className="list-group-item p-0 bg-transparent pt-2 pb-2">
                        <div>
                            <div className={`d-flex flex-row align-items-center`}>
                                <ToggleSwitch id={`concept_${id}_toggle_${card.id}_${coreConcept.name}_${idx}`}
                                    selected={mapKindToValue(cardConcepts[coreConcept.name]?.kind)}
                                    values={getConceptFlags(coreConcept)}                                                            
                                    valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                    displayNames={conceptFlagNames}
                                    onChange={val => onChange(coreConcept, mapValueToKind(val))} />
                                <div style={{ marginLeft: '15px' }}>{coreConcept.displayName}</div>
                            </div>
                        </div>

                        {coreConcept.childs.length > 0 &&
                            <ul className="">
                                {coreConcept.childs.map((childConcept, i) =>
                                    <>
                                        <li key={i}
                                            className={`d-flex flex-row align-items-centers mt-3`}>
                                            <ToggleSwitch id={`concept_${id}_toggle_${card.id}_${childConcept.name}_${idx}_${i}`}
                                                selected={mapKindToValue(cardConcepts[childConcept.name]?.kind)}
                                                values={getConceptFlags(childConcept)}
                                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                displayNames={conceptFlagNames}
                                                onChange={val => onChange(childConcept, mapValueToKind(val), coreConcept)} />
                                            <div style={{ marginLeft: '15px' }}>{childConcept.displayName}</div>
                                        </li>
                                    </>)}
                            </ul>}
                    </div>)}
        </>
    )
})

type ExerciseLawsProps = {    
    id: string | number,
    store: ExerciseSettingsStore,
    laws: DomainLaw[],
    cardLaws: Record<string, ExerciseCardLaw>,
    onChange: (law: DomainLaw, lawValue: ExerciseCardConceptKind, parent?: DomainLaw) => void,
}
const ExerciseLaws = observer((props: ExerciseLawsProps) => {
    const { id, store, laws, cardLaws, onChange } = props;
    const { t  } = useTranslation();
    const card = store.currentCard;
    if (!card)
        throw new Error('card not set');
    const user = useCurrentUser();   
    
    const lawFlagNames = useMemo(() => {
        return [t('exercisesettings_optDenied'), t('exercisesettings_optAllowed'), t('exercisesettings_optTarget')]
    }, [user?.language])

    return(
    <>
        {
            laws.map((coreLaw, idx) =>
                (<div className="list-group-item p-0 bg-transparent pt-2 pb-2" key={idx}>
                    <div>
                        <div className={`d-flex flex-row align-items-center`}>
                            <ToggleSwitch id={`law_${id}_toggle_${card.id}_${idx}`}
                                selected={mapKindToValue(cardLaws[coreLaw.name]?.kind)}
                                values={['Denied', 'Allowed', 'Target']}
                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                displayNames={lawFlagNames}
                                onChange={val => onChange(coreLaw, mapValueToKind(val))} />
                            <div style={{ marginLeft: '15px' }}>{coreLaw.displayName}</div>
                        </div>
                    </div>

                    {coreLaw.childs.length > 0 &&
                        <ul className="">
                            {coreLaw.childs.map((childLaw, i) =>
                                <>
                                    <li key={i} className={`d-flex flex-row align-items-centers mt-3`}>
                                    <ToggleSwitch id={`law_${id}_toggle_${card.id}_${idx}_${i}`}
                                        selected={mapKindToValue(cardLaws[childLaw.name]?.kind)}
                                        values={['Denied', 'Allowed', 'Target']}
                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                        displayNames={lawFlagNames}
                                        onChange={val => onChange(childLaw, mapValueToKind(val), coreLaw)} />
                                        <div style={{ marginLeft: '15px' }}>{childLaw.displayName}</div>
                                    </li>
                                </>)}
                        </ul>}
                </div>))
        }
    </>)
})

type ExerciseSkillsProps = {    
    id: string | number,
    store: ExerciseSettingsStore,
    skills: DomainSkill[],
    cardSkills: Record<string, ExerciseCardSkill>,
    onChange: (skill: DomainSkill, skillValue: ExerciseCardConceptKind, parent?: DomainSkill) => void,
}
const ExerciseSkills = observer((props: ExerciseSkillsProps) => {
    const { id, store, skills, cardSkills, onChange } = props;
    const { t  } = useTranslation();
    const card = store.currentCard;
    if (!card)
        throw new Error('card not set');   
    const user = useCurrentUser();
    
    const skillFlagNames = useMemo(() => {
        return [t('exercisesettings_optDenied'), t('exercisesettings_optAllowed'), t('exercisesettings_optTarget')]
    }, [user?.language])

    return(
    <>
        {
            skills.map((coreSkill, idx) =>
                (<div className="list-group-item p-0 bg-transparent pt-2 pb-2" key={idx}>
                    <div>
                        <div className={`d-flex flex-row align-items-center`}>
                            <ToggleSwitch id={`skill_${id}_toggle_${card.id}_${idx}`}
                                selected={mapKindToValue(cardSkills[coreSkill.name]?.kind)}
                                values={['Denied', 'Allowed', 'Target']}
                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                displayNames={skillFlagNames}
                                onChange={val => onChange(coreSkill, mapValueToKind(val))} />
                            <div style={{ marginLeft: '15px' }}>{coreSkill.displayName}</div>
                        </div>
                    </div>

                    {coreSkill.childs.length > 0 &&
                        <ul className="">
                            {coreSkill.childs.map((childSkill, i) =>
                                <>
                                    <li key={i} className={`d-flex flex-row align-items-centers mt-3`}>
                                    <ToggleSwitch id={`skill_${id}_toggle_${card.id}_${idx}_${i}`}
                                        selected={mapKindToValue(cardSkills[childSkill.name]?.kind)}
                                        values={['Denied', 'Allowed', 'Target']}
                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                        displayNames={skillFlagNames}
                                        onChange={val => onChange(childSkill, mapValueToKind(val), coreSkill)} />
                                        <div style={{ marginLeft: '15px' }}>{childSkill.displayName}</div>
                                    </li>
                                </>)}
                        </ul>}
                </div>))
        }
    </>)
})

function mapKindToValue(kind?: ExerciseCardConceptKind): 'Denied' | 'Allowed' | 'Target' {
    return kind === 'FORBIDDEN' ? 'Denied'
        : kind === 'TARGETED' ? 'Target' : 'Allowed'
}
function mapValueToKind(value?: 'Denied' | 'Allowed' | 'Target'): ExerciseCardConceptKind {
    return value === 'Denied' ? 'FORBIDDEN'
        : value === 'Target' ? 'TARGETED' : 'PERMITTED'
}
function getConceptFlags(c: DomainConcept): ['Denied', 'Allowed'] | ['Denied', 'Allowed', 'Target'] {
    return (c.bitflags & DomainConceptFlag.TargetEnabled) > 0 ? ['Denied', 'Allowed', 'Target'] : ['Denied', 'Allowed']
}
