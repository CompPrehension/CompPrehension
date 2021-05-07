import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import { MultiChoiceQuestion } from "../../../types/question";
import { ToggleSwitch } from "../toggle";

type MultiChoiceQuestionComponentProps = {
    question: MultiChoiceQuestion,
    answers: [number, number][],
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
    }
    return (<div>Not implemented</div>);
};


const SwitchMultiChoiceQuestionComponent = ({ question, answers, onChanged }: MultiChoiceQuestionComponentProps) => {    
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];

    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...answers.filter(v => v[0] !== answerId),
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
                                          selected={selectorTexts[answers.filter(h => h[0] === a.id)?.[0]?.[1]] ?? ""} 
                                          values={selectorTexts} 
                                          onChange={val => onSwitched(a.id, val)} />
                        </div>
                        <div>{a.text}</div>                        
                    </div>)}
            </p>
        </div>
    );
}

const SwitchMultiChoiceQuestionWithCtxComponent = ({ question, answers, onChanged }: MultiChoiceQuestionComponentProps) => {
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...answers.filter(v => v[0] !== answerId),
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
                                            selected={selectorTexts[answers.filter(h => h[0] === +id)?.[0]?.[1]] ?? ""} 
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
        answers.forEach(([id, value]) => {
            const inputId = `answer_${id}_${selectorTexts[value]}_checkbox`;
            const answr: any = document.getElementById(inputId);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.checked = true, 10)
            //answr.value = value;
        });
    }, [question.questionId, answers])
    
    return (
        <div>
            <p>
                <div dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>            
        </div>
    );
}
