import { observer } from "mobx-react";
import React, { useEffect } from "react";
import store from '../../../store';
import Select, { components } from 'react-select';
import ReactDOM from "react-dom";


export const MatchingQuestion : React.FC = observer(() => {
    const { questionData } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { options } = questionData;
    switch(true) {
        case options.displayMode === "combobox" && !options.requireContext:
            return <ComboboxMatchingQuestion />;
        case options.displayMode === "combobox" && options.requireContext:
            return <ComboboxMatchingQuestionWithCtx />;
        case options.displayMode === "dragNdrop":
            return <DragAndDropMatchingQuestion />;
        default:
            return <div>NonImplemented</div>;  
    }
});


const DragAndDropMatchingQuestion = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }
    const { answers = [], groups = [] } = questionData;

   
    return (<div>NonImplemented</div>);
});


const RawHtmlSelectOption = (props : any) => {
    const {innerRef, innerProps, children, ...rest} = props;
    return (   
        <components.Option {...rest}>
            <div ref={innerRef} {...innerProps} dangerouslySetInnerHTML={{__html: props.label}}></div>
        </components.Option>
    );
};

const RawHtmlSelectSingleValue = (props : any) => {
    const { innerRef, innerProps, children, ...rest } = props;
    return (   
        <components.SingleValue {...rest}>
            {props.getValue()?.map((v : any) => <div ref={innerRef} {...innerProps} dangerouslySetInnerHTML={{__html: v.label}}></div>)}
        </components.SingleValue>
    );
};

const ComboboxMatchingQuestionWithCtx = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING" || !questionData.options.requireContext) {
        return null;
    }
    const { groups = [], options } = questionData;      

    useEffect(() => {
        // replace all placeholders on first render
        document.querySelectorAll('[id^="answer_"]')
            .forEach(elem => {
                const answerId = +elem.id?.split("answer_")[1];
                const selector = <Select options={groups.map(g => ({ value: g.id, label: g.text }))}
                                         components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }} 
                                         onChange={(v => {
                                            if (!v) {
                                               return;
                                            }

                                            const state = store.answersHistory.reduce((acc, [l, r]) => (acc[l] = r, acc), {} as Record<number, number>);
                                            state[answerId] = v.value;
                                            store.updateAnswersHistory(Object.keys(state).map(k => [+k, +state[+k]]));
                                        })}
                                    />
                ReactDOM.render(selector, elem);
            });        
    }, [questionData.questionId]);

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: questionData.text }} />
        </div>
    );
});

const ComboboxMatchingQuestion = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING" || questionData.options.requireContext) {
        return null;
    }

    const { answers = [], groups = [], options } = questionData;        
    const currentState = answersHistory
        .reduce((acc, [l, r]) => (acc[l] = r, acc), {} as Record<number, number>); 

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: questionData.text }}>                
            </p>
            <table style={{ minWidth: '50%' }}>
                <tbody>
                    {answers.map(asw => 
                        <tr>
                            <td dangerouslySetInnerHTML={{ __html: asw.text}}></td>
                            <td>
                                <Select defaultValue={(currentState?.[asw.id] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                               return;
                                            }

                                            const state = currentState;
                                            state[asw.id] = v.value;
                                            store.updateAnswersHistory(Object.keys(state).map(k => [+k, +state[+k]]));
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
});
