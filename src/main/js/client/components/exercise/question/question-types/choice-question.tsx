import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import ReactDOM from "react-dom";
import { container } from "tsyringe";
import { ExerciseStore } from "../../../../stores/exercise-store";
import { ToggleSwitch } from "../../../common/toggle";

export const MultiChoiceQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    switch(true) {
        case options.displayMode == 'switch' && !options.requireContext:
            return <SwitchMultiChoiceQuestion />;
        case options.displayMode === 'switch' && options.requireContext:
            return <SwitchMultiChoiceQuestionWithCtx />;
    }
    return (<div>Not implemented</div>);
});

export const SingleChoiceQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion } = exerciseStore;
    if (!currentQuestion || currentQuestion.type !== 'SINGLE_CHOICE') {
        return null;
    }
    const options = currentQuestion.options;
    switch(true) {
        case options.displayMode == 'radio' && !options.requireContext:
            return <RadioSingleChoiceQuestion />;
        case options.displayMode === 'radio' && options.requireContext:
            return <RadioSingleChoiceQuestionWithCtx />;
    }
    return (<div>Not implemented</div>);
});

const SwitchMultiChoiceQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || currentQuestion.type !== 'MULTI_CHOICE') {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];

    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...exerciseStore.answersHistory.filter(v => v[0] !== answerId),
            [answerId, value] as [number, number],
        ];
        exerciseStore.updateAnswersHistory(newHistory);
    }
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
            </p>
            <p className="d-flex flex-column">                
                {currentQuestion.answers.map(a => 
                    <div className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <ToggleSwitch id={`anwser_${a.id}`} 
                                          selected={selectorTexts[answersHistory.filter(h => h[0] === a.id)?.[0]?.[1]] ?? ""} 
                                          values={selectorTexts} 
                                          onChange={val => onSwitched(a.id, val)} />
                        </div>
                        <div>{a.text}</div>                        
                    </div>)}
            </p>
        </div>
    );
})

const SwitchMultiChoiceQuestionWithCtx = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || currentQuestion.type !== 'MULTI_CHOICE') {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...exerciseStore.answersHistory.filter(v => v[0] !== answerId),
            [answerId, value] as [number, number],
        ];
        exerciseStore.updateAnswersHistory(newHistory);
    }

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const id = e.id?.split("answer_")[1] ?? -1;
            const component = <ToggleSwitch id={e.id} 
                                            selected={selectorTexts[answersHistory.filter(h => h[0] === +id)?.[0]?.[1]] ?? ""} 
                                            values={selectorTexts} 
                                            onChange={val => onSwitched(+id, val)} />
            ReactDOM.render(component, e);
            e.id = "";   
        })
    }, [currentQuestion.questionId]);

    // apply history changes
    useEffect(() => {
        // drop all changes
        document.querySelectorAll('input[id^="answer_"]').forEach((e: any) => {
            const id = +e.id?.split("answer_")[1] ?? -1;
            //if (!answersHistory.some(h => h[0] === id)) {
            e.checked = undefined;
            //}                
        });

        // apply history changes    
        answersHistory.forEach(([id, value]) => {
            const inputId = `answer_${id}_${selectorTexts[value]}_checkbox`;
            const answr: any = document.getElementById(inputId);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.checked = true, 10)
            //answr.value = value;
        });
    }, [currentQuestion.questionId, exerciseStore.answersHistory])
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
            </p>            
        </div>
    );
})


const RadioSingleChoiceQuestion = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || currentQuestion.type !== 'SINGLE_CHOICE') {
        return null;
    }
    const onChange = (answerId: number, checked: boolean) => {
        if (checked) {
            exerciseStore.updateAnswersHistory([[answerId, answerId]])
        }        
    }

    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
            </p>
            <p className="d-flex flex-column">                
                {currentQuestion.answers.map(a => 
                    <label htmlFor={`answer_${a.id}`} className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <input id={`answer_${a.id}`} 
                                   name={`switch_${currentQuestion.questionId}`} 
                                   type="radio" 
                                   checked={answersHistory.some(h => h[0] === a.id)}
                                   onChange={(e) => onChange(a.id, e.target.checked)} />
                        </div>
                        <div>{a.text}</div>                        
                    </label>)}
            </p>
        </div>
    );
})

const RadioSingleChoiceQuestionWithCtx = observer(() => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || currentQuestion.type !== 'SINGLE_CHOICE') {
        return null;
    }
    const options = currentQuestion.options;
    const onChange = (answerId: number, checked: boolean) => {
        if (checked) {
            exerciseStore.updateAnswersHistory([[answerId, answerId]])
        }        
    }

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const id = e.id?.split("answer_")[1] ?? -1;
            const component = (<label htmlFor={`answer_${id}`}>
                                 <input id={`answer_${id}`} 
                                        name={`switch_${currentQuestion.questionId}`} 
                                        type="radio" 
                                        checked={answersHistory.some(h => h[0] === +id)}
                                        onChange={(e) => onChange(+id, e.target.checked)} />
                                 <span dangerouslySetInnerHTML={{ __html: e.innerHTML }}/>
                               </label>)
            ReactDOM.render(component, e);
            e.id = "";   
        })
    }, [currentQuestion.questionId]);

    // apply history changes
    useEffect(() => {
        // drop all changes
        document.querySelectorAll('input[id^="answer_"]').forEach((e: any) => {
            e.checked = undefined;
        });

        // apply history changes    
        answersHistory.forEach(([id]) => {
            const answr: any = document.getElementById(`answer_${id}`);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.checked = true, 10)
        });
    }, [currentQuestion.questionId, exerciseStore.answersHistory])

    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
            </p>            
        </div>
    );
})
