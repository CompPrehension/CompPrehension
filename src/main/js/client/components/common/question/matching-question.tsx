import { Droppable, DroppableEventNames, Plugins } from "@shopify/draggable";
import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import Select, { components } from "react-select";
import { MatchingQuestion } from "../../../types/question";

type MatchingQuestionComponentProps = {
    question: MatchingQuestion,
    answers: [number, number][],
    onChanged: (newAnswers: [number, number][]) => void,
}

export const MatchingQuestionComponent = (props: MatchingQuestionComponentProps) => {    
    const { question } = props;
    const { options } = question;
    switch(true) {
        case options.displayMode === "combobox" && !options.requireContext:
            return <ComboboxMatchingQuestionComponent {...props}/>;
        case options.displayMode === "combobox" && options.requireContext:
            return <ComboboxMatchingQuestionWithCtxComponent {...props}/>;
        case options.displayMode === "dragNdrop":
            return <DragAndDropMatchingQuestionComponent {...props}/>;              
    }
    return (<div>Not Implemented</div>);
};

const DragAndDropMatchingQuestionComponent = ({ question, answers, onChanged }: MatchingQuestionComponentProps) => {
    if (question.options.displayMode !== 'dragNdrop') {
        return null;
    }
    const { groups = [] } = question;
    const { options } = question;

    // on question first render
    const dropzoneStyle = options.dropzoneStyle && JSON.parse(options.dropzoneStyle) || { minHeight: '40px', minWidth: '80px' };
    useEffect(() => {
        document.querySelectorAll('[id^="answer_"]')
            .forEach((e: any) => {
                e.classList.add("comp-ph-dropzone");
                Object.keys(dropzoneStyle).forEach(k => e.style[k] = dropzoneStyle[k]);
            });

        const droppable = new Droppable<DroppableEventNames>(document.querySelectorAll('.comp-ph-droppable-container'), {
            draggable: '.comp-ph-draggable',
            dropzone: '.comp-ph-dropzone',
            plugins: [Plugins.ResizeMirror],
        })

        droppable.on('droppable:stop', (e: any) => {
            const draggableId: string | undefined = e?.data?.dragEvent?.data?.source?.id;
            const droppableId: string | undefined = e?.data?.dropzone?.id;
            if (!draggableId || !droppableId) {
                return;
            }

            // clone draggable element and return it back
            if (options.multipleSelectionEnabled) {
                const wrapperId = `dragAnswerWrapper_${draggableId.split('_')[1] ?? ''}`;
                const wrapper = document.getElementById(wrapperId);
                const draggable = document.getElementById(draggableId);
                if (wrapperId !== droppableId && wrapper && draggable) {
                    wrapper.innerHTML = draggable.outerHTML;
                }
            }

            // setTimeout is needed to guarantee completion of all dnd events
            setTimeout(() => {
                const newHistory = [...(document.querySelectorAll('[id^="answer_"] > [id^="dragAnswer_"]') as unknown as Element[])]
                    .map<[number, number]>(e => {
                        const leftId = e.parentElement?.id.split('_')[1] ?? '';
                        const rightId = e?.id.split('_')[1] ?? '';
                        return [+leftId, +rightId];
                    });                        
                onChanged(newHistory);
            }, 10);
        });
    }, [question.questionId])
    
    return (
        <div>
            <div className="row">
                <div className="col-md">
                    <p className="comp-ph-droppable-container" dangerouslySetInnerHTML={{ __html: question.text }} />
                    {
                        !options.requireContext
                            ? <p className="d-flex flex-column comp-ph-droppable-container">
                                {question.answers.map(a =>
                                    <div className="d-flex flex-row mb-3">
                                        <div className="mr-2 mt-1">
                                            <div id={`answer_${a.id}`} className="comp-ph-dropzone" style={dropzoneStyle}></div>
                                        </div>
                                        <div dangerouslySetInnerHTML={{ __html: a.text}}></div>
                                    </div>)}
                            </p>
                            : null
                    }
                </div>
                <div className="col-md comp-ph-droppable-container d-flex justify-content-start align-items-start flex-column">
                    {groups.map(g => 
                        (<div id={`dragAnswerWrapper_${g.id}`} className="comp-ph-dropzone mb-2" style={dropzoneStyle}>
                            <div id={`dragAnswer_${g.id}`} className="comp-ph-draggable p-2" dangerouslySetInnerHTML={{ __html: g.text }}/>
                         </div>))}
                </div>
            </div>
        </div>);
};

const ComboboxMatchingQuestionComponent = ({ question, answers, onChanged }: MatchingQuestionComponentProps) => {    
    if (question.options.displayMode !== 'combobox') {
        return null;
    }

    const { groups = [], } = question;   

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: question.text }}>                
            </p>
            <table style={{ minWidth: '50%' }}>
                <tbody>
                    {question.answers.map(asw => 
                        <tr>
                            <td dangerouslySetInnerHTML={{ __html: question.text}}></td>
                            <td>
                                <Select defaultValue={(answers.find(v => v[0] === asw.id)?.[1] ?? null) as any}
                                        options={groups//.filter(g => !options.hideSelected || !Object.values(currentState).includes(g.id) || currentState[asw.id] == g.id)
                                                       .map(g => ({ value: g.id, label: g.text }))}
                                        components={{ Option: RawHtmlSelectOption, SingleValue: RawHtmlSelectSingleValue }}               
                                        onChange={(v => {
                                            if (!v) {
                                               return;
                                            }
                                            const otherHistoryItems = answers.filter(v => v[0] !== asw.id);
                                            const historyItem = [asw.id, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            onChanged(newAnswersHistory);                                            
                                        })} />                                
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>            
        </div>
    );
};

const ComboboxMatchingQuestionWithCtxComponent = ({ question, answers, onChanged }: MatchingQuestionComponentProps) => {
    if (question.options.displayMode !== 'combobox') {
        return null;
    }
    const { groups = [], options } = question;      

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

                                            const otherHistoryItems = answers.filter(v => v[0] !== answerId);
                                            const historyItem = [answerId, +v.value] as [number, number];
                                            const newAnswersHistory = [...otherHistoryItems, historyItem];
                                            onChanged(newAnswersHistory);     
                                        })}
                                    />
                ReactDOM.render(selector, elem);
            });        
    }, [question.questionId]);

    return (
        <div>
            <p dangerouslySetInnerHTML={{ __html: question.text }} />
        </div>
    );
};

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
