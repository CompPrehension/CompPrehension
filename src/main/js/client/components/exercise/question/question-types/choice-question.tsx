import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import ReactDOM from "react-dom";
import { container } from "tsyringe";
import { ExerciseStore } from "../../../../stores/exercise-store";
import { ToggleSwitch } from "../../../common/toggle";

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

const SwitchChoiceQuestion = observer(({ isMulti }: { isMulti : boolean }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];

    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        if (isMulti) {            
            const newHistory = [ 
                ...exerciseStore.answersHistory.filter(v => v[0] !== answerId),
                [answerId, value] as [number, number],
            ];
            exerciseStore.updateAnswersHistory(newHistory);
        } else if (value) {
            const newHistory = [
                [answerId, value] as [number, number],
                ...exerciseStore.currentQuestion?.answers.filter(a => a.id !== answerId).map(a => [a.id, 0] as [number, number]) ?? [],
            ];
            exerciseStore.updateAnswersHistory(newHistory);
        }
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

const SwitchChoiceQuestionWithCtx = observer(({ isMulti }: { isMulti : boolean }) => {
    const [exerciseStore] = useState(() => container.resolve(ExerciseStore));
    const { currentQuestion, answersHistory } = exerciseStore;
    if (!currentQuestion || (currentQuestion.type !== 'SINGLE_CHOICE' && currentQuestion.type !== 'MULTI_CHOICE')) {
        return null;
    }
    const options = currentQuestion.options;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        if (isMulti) {            
            const newHistory = [ 
                ...exerciseStore.answersHistory.filter(v => v[0] !== answerId),
                [answerId, value] as [number, number],
            ];
            exerciseStore.updateAnswersHistory(newHistory);
        } else if (value) {
            const answers = exerciseStore.currentQuestion?.text.match(/answer_\d(?=\W)/g)?.map(v => +v.split('_')[1]) ?? [];
            const newHistory = [
                [answerId, value] as [number, number],
                ...answers.filter(id => id !== answerId).map(id => [id, 0] as [number, number]) ?? [],
            ];
            exerciseStore.updateAnswersHistory(newHistory);
        }
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
            e.checked = undefined;
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

export const SingleChoiceQuestion = () => <ChoiceQuestion isMulti={false} />;
export const MultiChoiceQuestion = () => <ChoiceQuestion isMulti={true} />;
