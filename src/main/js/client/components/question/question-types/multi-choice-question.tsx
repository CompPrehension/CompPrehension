import React from "react";
import Checkbox from 'react-three-state-checkbox';
import { Question, QuestionAnswer } from "../../../typings/question.d";

export interface IMultiChoiceSelection {
    [id: string]: boolean | null,
}

export interface IMultiChoiceQuestionOptions {
    question?: Question,
    onSelectionChanged?: (selection: IMultiChoiceSelection) => void,
    selectedCheckboxes?: IMultiChoiceSelection,
}



export const MultiChoiceQuestion = (props: IMultiChoiceQuestionOptions) => {
    let { question, onSelectionChanged = () => { }, selectedCheckboxes = {} } = props;
    if (!question) {
        return null;
    }

    if (!Object.keys(selectedCheckboxes)) {
        selectedCheckboxes = question.answers.reduce((acc, v) => {
            acc[v.id] = null;
            return acc;
        }, {} as { [id: string]: boolean | null });
    }

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: question.text }} />
            {
                question.answers.map((a: QuestionAnswer) =>
                    <div>
                        <label>
                            <Checkbox checked={selectedCheckboxes[a.id] ?? false}
                                      indeterminate={selectedCheckboxes[a.id] === null} 
                                      onChange={e => onSelectionChanged({...selectedCheckboxes, [a.id]: e.target.checked })} />
                            <span id={`q${question?.id}_answ${a.id}_lbl`} dangerouslySetInnerHTML={{ __html: a.text }} />
                        </label>
                    </div>)
            }
        </div>
    );
}