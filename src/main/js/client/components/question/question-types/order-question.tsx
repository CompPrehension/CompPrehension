import React, { useEffect, useState } from "react";
import $ from 'jquery';
import { Question } from "../../../typings/question.d";


export interface IOrderQuestionOptions {
    question?: Question,
    onClicked?: (id: any) => void,
}

export const OrderQuestion = (props: IOrderQuestionOptions) => {
    const { question, onClicked = () => { } } = props;
    const [initialized, setInitialized] = useState(false);
    if (!question) {
        return null;
    }
    useEffect(() => {
        if (initialized)
            return;

        $('[id^="answer_"]')
            .click(e => {
                const idStr = $(e.target).attr('id')?.split("answer_")[1];
                console.log(idStr);
                onClicked(idStr);
            });
        setInitialized(true);
    });

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: question.text }} />
        </div>
    );
}