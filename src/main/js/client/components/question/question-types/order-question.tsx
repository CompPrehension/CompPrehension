import React, { useEffect, useState } from "react";
import $ from 'jquery';
import { Question } from "../../../typings/question.d";
import { observer } from "mobx-react";
import store from '../../../store';

export const OrderQuestion : React.FC = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData) {
        return null;
    }

    // при смене id добавляем обработчики нажатия
    const { options } = questionData;
    useEffect(() => {    
        $('[id^="answer_"]').each((_, e) => {
            const $this = $(e);
            const idStr = $this.attr('id')?.split("answer_")[1]; 
            $this.click(() => onAnswersChanged(idStr));
        });

        $("[data-comp-ph-pos]").each((_, e) => {
            const $this = $(e);
            const pos = $this.data("comp-ph-pos");
            $this.append($(`<span class="comp-ph-expr-top-hint">${pos}</span>`));
        })
    }, [questionData.id]);

    $('[id^="answer_"]').removeClass('disabled');
    $('.comp-ph-expr-bottom-hint').remove();
    answersHistory.forEach((h, idx) => {
        if (!options.multipleChoiceEnabled) {
            const answr = $(`#answer_${h}`);
            answr.addClass('disabled');
            answr.append($(`<span class="comp-ph-expr-bottom-hint">${idx}</span>`));
        }
    });

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: questionData.text }} />
        </div>
    );
});
