import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import { MatchingQuestion, MultiChoiceQuestion } from "../../../types/question";
import { ToggleSwitch } from "../toggle";
import { DragAndDropMatchingQuestionComponent } from "./matching-question";

type MultiChoiceQuestionComponentProps = {
    question: MultiChoiceQuestion,
    answers: [number, number][],
    getAnswers: () => [number, number][],
    onChanged: (newAnswers: [number, number][]) => void,
}

export const MultiChoiceQuestionComponent = (props: MultiChoiceQuestionComponentProps) => {
    const { question } = props;
    const { options } = question;
    switch(true) {
        case options.displayMode == 'switch' && !options.requireContext:
            return <SwitchMultiChoiceQuestionComponent {...props}/>;
        case options.displayMode === 'switch' && options.requireContext:
            return <SwitchMultiChoiceQuestionWithCtxComponent {...props}/>;
        case options.displayMode === 'dragNdrop':
            return <DndMultiChoiceQuestionComponent {...props}/>;
    }
    return (<div>Not implemented</div>);
};


const SwitchMultiChoiceQuestionComponent = ({ question, getAnswers, onChanged }: MultiChoiceQuestionComponentProps) => {    
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];

    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...getAnswers().filter(v => v[0] !== answerId),
            [answerId, value] as [number, number],
        ];
        onChanged(newHistory);
    }
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>
            <p className="d-flex flex-column">                
                {question.answers.map(a => 
                    <div className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <ToggleSwitch id={`anwser_${a.id}`} 
                                          selected={selectorTexts[getAnswers().filter(h => h[0] === a.id)?.[0]?.[1]] ?? ""} 
                                          values={selectorTexts} 
                                          onChange={val => onSwitched(a.id, val)} />
                        </div>
                        <div>{a.text}</div>                        
                    </div>)}
            </p>
        </div>
    );
}

const SwitchMultiChoiceQuestionWithCtxComponent = ({ question, getAnswers, onChanged }: MultiChoiceQuestionComponentProps) => {
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...getAnswers().filter(v => v[0] !== answerId),
            [answerId, value] as [number, number],
        ];
        onChanged(newHistory);
    }

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll('[id^="answer_"]').forEach(e => {
            const id = e.id?.split("answer_")[1] ?? -1;
            const component = <ToggleSwitch id={e.id} 
                                            selected={selectorTexts[getAnswers().filter(h => h[0] === +id)?.[0]?.[1]] ?? ""} 
                                            values={selectorTexts} 
                                            onChange={val => onSwitched(+id, val)} />
            ReactDOM.render(component, e);
            e.id = "";   
        })
    }, [question.questionId]);

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
        getAnswers().forEach(([id, value]) => {
            const inputId = `answer_${id}_${selectorTexts[value]}_checkbox`;
            const answr: any = document.getElementById(inputId);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.checked = true, 10)
            //answr.value = value;
        });
    }, [question.questionId, getAnswers().map(v => `${v[0]}${v[1]}`).join('')])
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>            
        </div>
    );
}


const DndMultiChoiceQuestionComponent = ({ question, getAnswers, onChanged, answers }: MultiChoiceQuestionComponentProps) => {
    if (question.options.displayMode !== 'dragNdrop') {
        return null;
    }
    
    const matchingQuestion: MatchingQuestion = {
        ...question,
        type: 'MATCHING',
        options: {
            ...question.options,
            multipleSelectionEnabled: true,
        },
        groups: [
            {
                id: 0,
                text: `<span class="badge badge-success" style="height: 100%; width: 100%;display: flex; align-items: center;justify-content: center;">✓</span>`,
            },
            {
                id: 1,
                text: `<span class="badge badge-danger" style="height: 100%; width: 100%;display: flex; align-items: center;justify-content: center;">x</span>`,
            },
        ],
    }

    return (<DragAndDropMatchingQuestionComponent question={matchingQuestion} getAnswers={getAnswers} onChanged={onChanged} answers={answers}/>)
}
