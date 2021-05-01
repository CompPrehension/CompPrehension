import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import ReactDOM from "react-dom";
import { container } from "tsyringe";
import { ExerciseStore } from "../../../../stores/exercise-store";
//@ts-ignore
import TriStateToggleSwitch from 'rn-tri-toggle-switch'

const ChoiceQuestion = observer((props: { isMulti : boolean }) => {
    const { isMulti } = props;
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    switch(true) {
        case options.displayMode == 'switch' && !options.requireContext:
            return <SwitchChoiceQuestion isMulti={isMulti}/>;
        case options.displayMode === 'switch' && options.requireContext:
            return <SwitchChoiceQuestionWithCtx isMulti={isMulti}/>;
    }
    return (<div>Not implemented</div>);
})

const SwitchChoiceQuestion = observer((props: { isMulti : boolean }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["yes", "no"];
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: currentQuestion.text }} />
            </p>
            <p className="d-flex flex-column">                
                {currentQuestion.answers.map(a => 
                    <div className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <SwitchBlock id={`anwser_${a.id}`} 
                                         value={answersHistory.filter(h => h[0] === a.id)?.[0]?.[1] ?? ""} 
                                         options={selectorTexts} 
                                         onChange={e => onSelectionChanged(a.id, +e.target.value)} />
                        </div>
                        <div>{a.text}</div>                        
                    </div>)}
            </p>
        </div>
    );
})

const SwitchChoiceQuestionWithCtx = observer((props: { isMulti : boolean }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["yes", "no"];

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const id = e.id?.split("answer_")[1] ?? -1;
            const component = <SwitchBlock id={e.id} 
                                           value={answersHistory.filter(h => h[0] === +id)?.[0]?.[1] ?? ""} 
                                           options={selectorTexts} 
                                           onChange={e => onSelectionChanged(+id, +e.target.value)} />
            ReactDOM.render(component, e);
            e.id = "";         
        })
    }, [currentQuestion.questionId]);

    // apply history changes
    useEffect(() => {
        // drop all changes
        document.querySelectorAll('[id^="answer_"]').forEach((e: any) => {
            e.value = "";
        });

        // apply history changes    
        answersHistory.forEach(([id, value]) => {
            const answr: any = document.querySelector(`#answer_${id}`);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.value = value, 10)
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

type SwitchBlockProps = {
    id: string, 
    value: string | number, 
    options: (string | number)[], 
    onChange: (e: any) => void,
}
const SwitchBlock = ({ id, value, options, onChange }: SwitchBlockProps) => {    
    return (
        <select id={id} value={value} onChange={onChange}>
            {<option value="" disabled></option>}
            {options.map((t, idx) => <option value={idx}>{t}</option>)}
        </select>
    )
};

const onSelectionChanged = (answerId: number, value: number) => {
    const exerciseStore = container.resolve(ExerciseStore);
    const newHistory = [ 
        ...exerciseStore.answersHistory.filter(v => v[0] !== answerId),
        [answerId, value] as [number, number],
    ];
    exerciseStore.updateAnswersHistory(newHistory, false);
}

export const SingleChoiceQuestion = () => <ChoiceQuestion isMulti={false} />;
export const MultiChoiceQuestion = () => <ChoiceQuestion isMulti={true} />;
