import { observer } from "mobx-react";
import React from "react";
import store from '../../../store';
import Select, { components } from 'react-select';


export const MatchingQuestion : React.FC = observer(() => {
    const { questionData } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { options } = questionData;
    switch(options.displayMode) {
        case "combobox":
            return <ComboboxMatchingQuestion />;
        case "dragNdrop":
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

const ComboboxMatchingQuestion = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "MATCHING") {
        return null;
    }

    const { answers = [], groups = [], options } = questionData;
        
    let currentState : Record<string, any> = {};  
    if (answersHistory.length) {
        currentState = JSON.parse(answersHistory[answersHistory.length - 1]) as unknown as Record<string, any>;
    }

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
                                <Select defaultValue={currentState?.[asw.id] ?? null}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (v !== null) {
                                                const state = currentState;
                                                state[asw.id] = v.value;
                                                store.onAnswersChanged(JSON.stringify(state));
                                            }
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
});
