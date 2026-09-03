import parse from "html-react-parser";
import { observer } from "mobx-react";
import { Answer } from "../../../types/answer";
import { MatchingQuestion, MultiChoiceQuestion } from "../../../types/question";
import { ToggleSwitch } from "../toggle";
import { answerSlotId } from "./answer-slot";
import { DragAndDropMatchingQuestionComponent } from "./matching-question";

type MultiChoiceQuestionComponentProps = {
    question: MultiChoiceQuestion,
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (newAnswers: Answer[]) => void,
}

export const MultiChoiceQuestionComponent = observer((props: MultiChoiceQuestionComponentProps) => {
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
});


const SwitchMultiChoiceQuestionComponent = observer((props: MultiChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];

    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...getAnswers().filter(v => v.answer[0] !== answerId),
            { answer: [answerId, value] as [number, number], isСreatedByUser: true },
        ];
        onChanged(newHistory);
    }
    
    return (
        <div>
            <p>
                <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>
            <p className="d-flex flex-column">                
                {question.answers.map(a => 
                    <div className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <ToggleSwitch id={`question_${question.questionId}_anwser_${a.id}`} 
                                          selected={selectorTexts[getAnswers().filter(h => h.answer[0] === a.id)?.[0]?.answer?.[1]] ?? ""} 
                                          values={selectorTexts} 
                                          inputAttributes={{ 'data-answer-id': a.id }}
                                          onChange={val => onSwitched(a.id, val)} />
                        </div>
                        <div>{a.text}</div>                        
                    </div>)}
            </p>
        </div>
    );
})

const SwitchMultiChoiceQuestionWithCtxComponent = observer((props: MultiChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'switch') {
        return null;
    }
    const { options } = question;
    const selectorTexts = options.selectorReplacers ?? ["no", "yes"];
    const onSwitched = (answerId: number, val: string) => {
        const value = selectorTexts.indexOf(val);
        const newHistory = [ 
            ...getAnswers().filter(v => v.answer[0] !== answerId),
            { answer: [answerId, value] as [number, number], isСreatedByUser: true, },
        ];
        onChanged(newHistory);
    }

    const content = parse(question.text, {
        replace: (node) => {
            const id = answerSlotId(node);
            if (id === null) {
                return;
            }

            return (
                <ToggleSwitch id={`toggle_answer_${id}`}
                              selected={selectorTexts[getAnswers().filter(h => h.answer[0] === id)?.[0]?.answer?.[1]] ?? ""}
                              inputAttributes={{ 'data-answer-id': id }}
                              values={selectorTexts}
                              onChange={val => onSwitched(id, val)} />
            );
        },
    });

    return (
        <div id={`question_${question.questionId}`}>
            <div className="comp-ph-question-text">{content}</div>
        </div>
    );
})


const DndMultiChoiceQuestionComponent = observer((props: MultiChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged, answers } = props;
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
})
