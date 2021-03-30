import { observer } from "mobx-react";
import React, { useEffect } from "react";
import store from '../../../store';
import Select, { components } from 'react-select';
import ReactDOM from "react-dom";

// @ts-ignore
import { Droppable } from '@shopify/draggable';


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
    const { groups = [] } = questionData;



    // on question first render
    const dropzoneStyle = { height: '40px', width: '80px' };
    useEffect(() => {
        document.querySelectorAll('[id^="answer_"]')
            .forEach((e: any) => {
                e.classList.add("comp-ph-dropzone");
                e.style.height = dropzoneStyle.height;
                e.style.width = dropzoneStyle.width;
            });

        const droppable = new Droppable(document.querySelectorAll('.comp-ph-droppable-container'), {
            draggable: '.comp-ph-draggable',
            dropzone: '.comp-ph-dropzone',
        })

        droppable.on('droppable:dropped', (e: any) => {             
            const source = e?.data?.dragEvent?.data?.originalSource;
            const target = e?.data?.dropzone;
            console.log('dropped', source, target)
        });
    }, [questionData.questionId])
    
    return (
        <div>
            <div className="row comp-ph-droppable-container">
                <div className="col-md">
                    <p dangerouslySetInnerHTML={{ __html: questionData.text }} />
                    <div id="answer_0" className="comp-ph-dropzone" style={dropzoneStyle}></div>
                </div>
                <div className="col-md">
                    {groups.map(g => 
                        (<div className="comp-ph-dropzone draggable-dropzone--occupied d-flex flex-column" style={dropzoneStyle}>
                            <div id={`dragAnswer_${g.id}`} className="comp-ph-draggable p-2 d-flex justify-content-center" dangerouslySetInnerHTML={{ __html: g.text }}/>
                         </div>))}
                </div>
            </div>
        </div>);
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

                                            const otherHistoryItems = answersHistory.filter(v => v[0] !== answerId);
                                            const historyItem = [answerId, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            store.updateAnswersHistory(newAnswersHistory);     
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
                                <Select defaultValue={(answersHistory.find(v => v[0] === asw.id)?.[1] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                               return;
                                            }
                                            const otherHistoryItems = answersHistory.filter(v => v[0] !== asw.id);
                                            const historyItem = [asw.id, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            store.updateAnswersHistory(newAnswersHistory);                                            
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
            
        </div>
    );
});
