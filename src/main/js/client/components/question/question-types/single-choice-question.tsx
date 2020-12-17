
import React from "react";
import Checkbox from "@material-ui/core/Checkbox";
import { Question } from "../../../typings/question.d";

export interface ISingleChoiceQuestionOptions {
    question?: Question,
    onSelectionChanged?: (idx: string) => void,
    selectedCheckboxId?: string,
}

export const SingleChoiceQuestion = (props: ISingleChoiceQuestionOptions) => {
    const { question, onSelectionChanged = () => { }, selectedCheckboxId = -1 } = props;
    if (!question) {
        return null;
    }

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: question.text }} />
            {
                question.answers.map(a =>
                    <div>
                        <label>
                            <Checkbox checked={selectedCheckboxId === a.id}
                                onChange={(_, ch) => (ch && (a.id !== selectedCheckboxId)) ? onSelectionChanged(a.id) : null}
                                id={`q${question.id}_answ${a.id}`} />
                            <span id={`q${question.id}_answ${a.id}_lbl`} dangerouslySetInnerHTML={{ __html: a.text }} />
                        </label>
                    </div>
                )
            }
        </div>
    );
}