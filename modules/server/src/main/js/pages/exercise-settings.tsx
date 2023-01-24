import React, { useCallback, useEffect, useMemo, useState } from "react";
import { container } from "tsyringe";
import { ExerciseSettingsController } from "../controllers/exercise/exercise-settings";
import * as E from "fp-ts/lib/Either";
import { Domain, DomainConcept, DomainConceptFlag, DomainLaw, ExerciseCard, ExerciseCardConcept, ExerciseCardConceptKind, ExerciseCardLaw, ExerciseCardViewModel, ExerciseListItem, Strategy } from "../types/exercise-settings";
import { ExerciseSettingsStore } from "../stores/exercise-settings-store";
import { observer } from "mobx-react";
import { ToggleSwitch } from "../components/common/toggle";
import { Link } from "react-router-dom";
import { Loader } from "../components/common/loader";
import { Modal } from "../components/common/modal";
import { boolean } from "io-ts";
import { useTranslation } from "react-i18next";
import { Header } from "../components/common/header";

export const ExerciseSettings = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseSettingsStore));
    const { t } = useTranslation();
    useEffect(() => {
        (async () => {
            await exerciseStore.loadExercises();

            const currentExercise = new URL(window.location.href).searchParams.get("exerciseId");
            if (currentExercise) {
                await exerciseStore.loadExercise(Number.parseInt(currentExercise));
            }
        })()
    }, []);

    const onNewExerciseClicked = useCallback(() => {
        (async () => {
            await exerciseStore.createNewExecise();
        })()
    }, [exerciseStore.exercises?.length]);

    /*
    const onLangClicked = useCallback(() => {
        const currentLang = exerciseStore.user?.language;
        const newLang = currentLang === "RU" ? "EN" : "RU";
        exerciseStore.changeLanguage(newLang);
    }, [exerciseStore])
    */

    if (exerciseStore.exercisesLoadStatus === 'LOADING') {
        return <Loader />;
    }

    const { user } = exerciseStore;
    if (!user)
        return <Loader />;

    return (
        <div className="container-fluid">
            <div className="pt-1 pb-3">
                <Header text={t('exercisesettings_title')}
                        languageHint={t('language_header')}
                        language={exerciseStore.user?.language ?? "EN"}
                        onLanguageClicked={/*onLangClicked*/null}
                        userHint={t('signedin_as_header')}
                        user={user.displayName} 
                        userHref={/*`${window.location.origin}/logout`*/null} />
            </div>
            <div className="flex-xl-nowrap row">
                <div className="col-xl-3 col-md-3 col-12 d-flex flex-column">
                    <button type="button" className="btn btn-primary mb-3" onClick={onNewExerciseClicked}>Create new</button>
                    <ul className="list-group">
                        {exerciseStore.exercises?.map(e =>
                            <Link key={e.id}
                                className={`list-group-item ${e.id === exerciseStore.currentCard?.id && "active" || ""}`}
                                to={`?exerciseId=${e.id}`}
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
}

const ExerciseCardElement = observer((props: ExerciseCardElementProps) => {
    const { card, domains, backends, strategies, store } = props;
    const { t } = useTranslation();    
    const conceptFlagNames = useMemo(() => {
        return [t('exercisesettings_optDenied'), t('exercisesettings_optAllowed'), t('exercisesettings_optTarget')]
    }, [store.user?.language])

    if (store.exercisesLoadStatus === 'EXERCISELOADING')
        return <Loader delay={200} />;

    if (card == null)
        return (<div>No exercise selected</div>);

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

    const stageDomainLaws = domains.find(z => z.id === card.domainId)?.laws
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
    const stageDomainConcepts = domains.find(z => z.id === card.domainId)?.concepts
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) > 0);
    const cardLaws = card.stages[0].laws.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardLaw>);
    const cardConcepts = card.stages[0].concepts.reduce((acc, i) => (acc[i.name] = i, acc), {} as Record<string, ExerciseCardConcept>);    
    const sharedDomainLaws = domains.find(z => z.id === card.domainId)?.laws
        .filter(l => (l.bitflags & DomainConceptFlag.TargetEnabled) === 0);
    const sharedDomainConcepts = domains.find(z => z.id === card.domainId)?.concepts
        .filter(c => (c.bitflags & DomainConceptFlag.TargetEnabled) === 0);
    const currentStrategy = strategies.find(s => s.id === card.strategyId);

    return (
        <div>
            <form className="exercise-settings-form">
                <div className="form-group">
                    <label className="font-weight-bold" htmlFor="exampleInputEmail1">{t('exercisesettings_name')}</label>
                    <input value={card.name} type="email" className="form-control" id="exampleInputEmail1" aria-describedby="emailHelp" placeholder="Enter email" onChange={e => store.setCardName(e.target.value)} />
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_domain')}</label>
                    <select className="form-control" value={card.domainId} onChange={e => store.setCardDomain(e.target.value)}>
                        {domains?.map(d => <option>{d.name}</option>)}
                    </select>
                </div>
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_strategy')}</label>
                    <select className="form-control" value={card.strategyId} onChange={e => store.setCardStrategy(e.target.value)}>
                        {strategies?.map(d => <option value={d.id}>{d.id}</option>)}
                    </select>
                </div>
                <div className="row">
                    <div className="col-md-6">
                        <div className="form-group">
                            <label className="font-weight-bold">{t('exercisesettings_qcomplexity')}</label>
                            <div>
                                <input type="range"
                                    className="form-control-range"
                                    id="formControlRange1"
                                    value={(store.currentCard?.complexity ?? 0.5) * 100}
                                    onChange={e => store.setCardQuestionComplexity(e.target.value)} />
                            </div>
                        </div>
                    </div>
                </div>

                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_qopt')}</label>
                    <div className="form-check">
                        <input checked={card.options.forceNewAttemptCreationEnabled} 
                               onChange={x => store.setCardFlag('forceNewAttemptCreationEnabled', x.target.checked)} 
                               type="checkbox" className="form-check-input" id="forceNewAttemptCreationEnabled" />
                        <label className="form-check-label" htmlFor="forceNewAttemptCreationEnabled">{t('exercisesettings_qopt_forceAttCreation')}</label>
                    </div>
                    <div className="form-check">
                        <input checked={card.options.correctAnswerGenerationEnabled} 
                               onChange={x => store.setCardFlag('correctAnswerGenerationEnabled', x.target.checked)} 
                               type="checkbox" className="form-check-input" id="correctAnswerGenerationEnabled" />
                        <label className="form-check-label" htmlFor="correctAnswerGenerationEnabled">{t('exercisesettings_qopt_genCorAnsw')}</label>
                    </div>
                    <div className="form-check">
                        <input checked={card.options.newQuestionGenerationEnabled} 
                               onChange={x => store.setCardFlag('newQuestionGenerationEnabled', x.target.checked)} 
                               type="checkbox" className="form-check-input" id="newQuestionGenerationEnabled" />
                        <label className="form-check-label" htmlFor="newQuestionGenerationEnabled">{t('exercisesettings_qopt_forceShowGenNextQ')}</label>
                    </div>
                    <div className="form-check">
                        <input checked={card.options.supplementaryQuestionsEnabled} 
                               onChange={x => store.setCardFlag('supplementaryQuestionsEnabled', x.target.checked)} 
                               type="checkbox" className="form-check-input" id="supplementaryQuestionsEnabled" />
                        <label className="form-check-label" htmlFor="supplementaryQuestionsEnabled">{t('exercisesettings_qopt_supQ')}</label>
                    </div>
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
                        <input id="survOptions"
                            type="text"
                            value={card.options.surveyOptions?.surveyId}
                            className="form-control"
                            aria-label="Text input with checkbox"
                            disabled={!card.options.surveyOptions?.enabled}
                            onChange={x => store.setCardSurveyId(x.target.value)} />
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="exTagsValues" className="font-weight-bold">{t('exercisesettings_tags')}</label>
                    <input id="exTagsValues" value={card.tags} className="form-control" onChange={v => store.setCardTags(v.target.value)} />
                </div>
                {sharedDomainConcepts?.length
                    && <div className="form-group">
                        <div>
                            <label className="font-weight-bold">{t('exercisesettings_commonConcepts')}</label>
                        </div>
                        <div className="row">
                            <div className="col-md-12">
                                <div className="list-group list-group-flush">
                                    {sharedDomainConcepts
                                        .map((coreConcept, idx) =>
                                            <div key={idx} className="list-group-item p-0 bg-transparent pt-2 pb-2">
                                                <div>
                                                    <div className={`d-flex flex-row align-items-center`}>
                                                        <ToggleSwitch id={`sharedconcept_toggle_${card.id}_${coreConcept.name}_${idx}`}
                                                            selected={mapKindToValue(cardConcepts[coreConcept.name]?.kind)}
                                                            values={getConceptFlags(coreConcept)}                                                            
                                                            valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                            displayNames={conceptFlagNames}
                                                            onChange={val => {
                                                                store.setCardCommonConceptValue(coreConcept.name, mapValueToKind(val))
                                                                coreConcept.childs.forEach(c => store.setCardCommonConceptValue(c.name, mapValueToKind(val)));
                                                            }} />
                                                        <div style={{ marginLeft: '15px' }}>{coreConcept.displayName}</div>
                                                    </div>
                                                </div>

                                                {coreConcept.childs.length > 0 &&
                                                    <ul className="">
                                                        {coreConcept.childs.map((childConcept, i) =>
                                                            <>
                                                                <li key={i}
                                                                    className={`d-flex flex-row align-items-centers mt-3`}>
                                                                    <ToggleSwitch id={`sharedconcept_toggle_${card.id}_${childConcept.name}_${idx}`}
                                                                        selected={mapKindToValue(cardConcepts[childConcept.name]?.kind)}
                                                                        values={getConceptFlags(childConcept)}
                                                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                                        displayNames={conceptFlagNames}
                                                                        onChange={val => {
                                                                            store.setCardCommonConceptValue(childConcept.name, mapValueToKind(val))
                                                                            store.setCardCommonConceptValue(coreConcept.name, 'PERMITTED')
                                                                        }} />
                                                                    <div style={{ marginLeft: '15px' }}>{childConcept.displayName}</div>
                                                                </li>
                                                            </>)}
                                                    </ul>}
                                            </div>)}
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
                            {sharedDomainLaws
                                .map((c, idx) =>
                                (<div className="list-group-item p-0 bg-transparent pt-2 pb-2" key={idx}>
                                    <div className="d-flex flex-row align-items-start justify-content-start">
                                        <div>
                                            <ToggleSwitch id={`sharedlaw_toggle_${card.id}_${idx}`}
                                                selected={mapKindToValue(cardLaws[c.name]?.kind)}
                                                values={['Denied', 'Allowed', 'Target']}
                                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                displayNames={conceptFlagNames}
                                                onChange={val => store.setCardCommonLawValue(c.name, mapValueToKind(val))} />
                                        </div>
                                        <div style={{ marginLeft: '15px' }}>{c.displayName}</div>
                                    </div>
                                </div>))}
                        </div>
                    </div>
                    || null
                }
                <div className="form-group">
                    <label className="font-weight-bold">{t('exercisesettings_stages')}</label>
                    <div className="list-group list-group-flush">
                        {card.stages.map((stage, stageIdx, stages) =>
                            <>
                                <div className="card mb-3">
                                    {stages.length > 1 &&
                                        <button type="button" 
                                                className="close" 
                                                aria-label="Close" 
                                                style={{'position': 'absolute', 'top': '10px', 'right': '10px'}}
                                                onClick={() => store.removeStage(stageIdx)}>
                                            <span aria-hidden="true">&times;</span>
                                        </button>
                                        || null
                                    }                                    
                                    <div className="card-body">
                                        <div className="form-group">{t('exercisesettings_stageN', { stageNumber: stageIdx + 1})}</div>
                                        {currentStrategy?.options.multiStagesEnabled
                                            && <div className="form-group">
                                                    <label className="font-weight-bold" htmlFor={`numberOfQuestions_stage${stageIdx}`}>{t('exercisesettings_stageN_qnumber')}</label>
                                                    <input type="text"
                                                        className="form-control"
                                                        id={`numberOfQuestions_stage${stageIdx}`}
                                                        value={stage.numberOfQuestions}
                                                        onChange={e => store.setCardStageNumberOfQuestions(stageIdx, e.target.value)} />
                                               </div>
                                            || null
                                        }                                        
                                        {(stageDomainConcepts && stageDomainConcepts.length > 0) &&
                                            <div className="form-group">
                                                <label className="font-weight-bold">{t('exercisesettings_stageN_concepts')}</label>
                                                <div className="list-group list-group-flush">
                                                    {stageDomainConcepts
                                                        ?.map((coreConcept, idx) =>
                                                            <div key={idx} className="list-group-item p-0 bg-transparent pt-2 pb-2">
                                                                <div>
                                                                    <div
                                                                        className={`d-flex flex-row align-items-center`}>
                                                                        <ToggleSwitch id={`stageconcept${stageIdx}_toggle_${card.id}_${coreConcept.name}_${idx}`}
                                                                            selected={mapKindToValue(stage.concepts.find(l => l.name === coreConcept.name)?.kind)}
                                                                            values={getConceptFlags(coreConcept)}
                                                                            valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                                            displayNames={conceptFlagNames}
                                                                            onChange={val => {
                                                                                store.setCardStageConceptValue(stageIdx, coreConcept.name, mapValueToKind(val))
                                                                                coreConcept.childs.forEach(c => store.setCardStageConceptValue(stageIdx, c.name, mapValueToKind(val)));
                                                                            }} />
                                                                        <div style={{ marginLeft: '15px' }}>{coreConcept.displayName}</div>
                                                                    </div>
                                                                </div>
                                                                {coreConcept.childs.length > 0 &&
                                                                    <ul className="">
                                                                        {coreConcept.childs.map((childConcept, i) =>
                                                                            <>
                                                                                <li key={i}
                                                                                    className={`d-flex flex-row align-items-centers mt-3`}>
                                                                                    <ToggleSwitch id={`stageconcept${stageIdx}_toggle_${card.id}_${childConcept.name}_${idx}`}
                                                                                        selected={mapKindToValue(stage.concepts.find(l => l.name === childConcept.name)?.kind)}
                                                                                        values={getConceptFlags(childConcept)}
                                                                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                                                        displayNames={conceptFlagNames}
                                                                                        onChange={val => {
                                                                                            store.setCardStageConceptValue(stageIdx, childConcept.name, mapValueToKind(val))
                                                                                            store.setCardStageConceptValue(stageIdx, coreConcept.name, 'PERMITTED')
                                                                                        }} />
                                                                                    <div style={{ marginLeft: '15px' }}>{childConcept.displayName}</div>
                                                                                </li>
                                                                            </>)}
                                                                    </ul>}
                                                            </div>)}
                                                </div>
                                            </div>
                                            || null}
                                        {(stageDomainLaws && stageDomainLaws.length > 0)
                                            &&
                                            <div className="form-group">
                                                <label className="font-weight-bold">{t('exercisesettings_stageN_laws')}</label>
                                                <div className="list-group list-group-flush">
                                                    {stageDomainLaws.map((c, idx) =>
                                                    (<div className="list-group-item p-0 bg-transparent pt-2 pb-2" key={idx}>
                                                        <div className="d-flex flex-row align-items-start justify-content-start">
                                                            <div>
                                                                <ToggleSwitch id={`stagelaw${stageIdx}_toggle_${card.id}_${idx}`}
                                                                    selected={mapKindToValue(stage.laws.find(l => l.name === c.name)?.kind)}
                                                                    values={['Denied', 'Allowed', 'Target']}
                                                                    valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                                    displayNames={conceptFlagNames}
                                                                    onChange={val => store.setCardStageLawValue(stageIdx, c.name, mapValueToKind(val))} />
                                                            </div>
                                                            <div style={{ marginLeft: '15px' }}>{c.displayName}</div>
                                                        </div>
                                                    </div>))}
                                                </div>
                                            </div>
                                            || null
                                        }
                                    </div>
                                </div>
                            </>)}
                    </div>
                </div>
                {card.stages.length < 5 && currentStrategy?.options.multiStagesEnabled
                    ? <div style={{marginTop: "-1rem"}}>
                        <button type="button" className="btn btn-success" onClick={() => store.addStage()}>{t('exercisesettings_addStage')}</button>
                      </div>
                    : null
                }
            </form >
            <div className="mt-5">
                <button type="button" className="btn btn-primary" onClick={() => store.saveCard()}>{t('exercisesettings_save')}</button>
                <button type="button" className="btn btn-primary ml-2" onClick={() => store.saveCard().then(() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}`, '_blank')?.focus()) }>{t('exercisesettings_saveNopen')}</button>
                <button type="button" className="btn btn-primary ml-2" onClick={() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}`, '_blank')?.focus()}>{t('exercisesettings_open')}</button>
                {currentStrategy?.options.multiStagesEnabled &&
                    <button type="button" className="btn btn-primary ml-2" onClick={() => window.open(`${window.location.origin}/pages/exercise?exerciseId=${card.id}&debug`, '_blank')?.focus()}>{t('exercisesettings_genDebugAtt')}</button>
                }
                </div>
        </div >


    );
})