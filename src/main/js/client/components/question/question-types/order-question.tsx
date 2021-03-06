import React, { useEffect, useState } from "react";
import $ from 'jquery';
import { observer } from "mobx-react";
import store from '../../../store';

export const OrderQuestion : React.FC = observer(() => {
    const { questionData, answersHistory, onAnswersChanged } = store;
    if (!questionData || questionData.type != "ORDER" || !questionData.options.requireContext) {
        return null;
    }

    const { options } = questionData; 
    const orderNumberOptions = options.orderNumberOptions ?? { delimiter: '/', position: 'SUFFIX', }
    const originalText = $(questionData.text);
    
    // actions on questionId changed (onInit)
    useEffect(() => {    
        // add button click event handlers
        $('[id^="answer_"]').each((_, e) => {
            const $this = $(e);
            const idStr = $this.attr('id')?.split("answer_")[1] ?? ""; 
            $this.click(() => onAnswersChanged(idStr));
        });

        // show elements positions
        $("[data-comp-ph-pos]").each((_, e) => {
            const $this = $(e);
            const pos = $this.data("comp-ph-pos");
            $this.append($(`<span class="comp-ph-expr-top-hint">${pos}</span>`));
        });  
    }, [questionData.id]);


    // drop all changes, set original qustion text
    $('[id^="answer_"]')
        .each((_, e) => {
            const $this = $(e);
            const pos = $this.data("comp-ph-pos");
            $this.html(originalText.find(`#${e.id}`).html());
            $this.append($(`<span class="comp-ph-expr-top-hint">${pos}</span>`));
        })
        .removeClass('disabled'); 

    // apply history changes          
    answersHistory.forEach((h, idx) => {
        const answr = $(`#answer_${h}`);

        // add pos hint        
        if (orderNumberOptions.position !== 'NONE') {
            const orderNumber = orderNumberOptions.replacers?.[idx] ?? (idx + 1);
            const answerHtml = orderNumberOptions.position === 'PREFIX' ? [orderNumber, answr.html()] :
                               orderNumberOptions.position === 'SUFFIX' ? [answr.html(), orderNumber] : [answr.html()];            
            answr.html(answerHtml.join(orderNumberOptions.delimiter));
        }        

        // disable if needed
        if (options.multipleSelectionEnabled) {            
            answr.addClass('disabled');            
        }
    });

    return (
        <div>
            <div dangerouslySetInnerHTML={{ __html: questionData.text }} />
        </div>
    );
});
