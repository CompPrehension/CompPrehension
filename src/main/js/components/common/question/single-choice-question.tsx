import { observer } from "mobx-react";
import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import { Answer } from "../../../types/answer";
import { SingleChoiceQuestion } from "../../../types/question";

type SingleChoiceQuestionComponentProps = {
    question: SingleChoiceQuestion,
    answers: Answer[],
    getAnswers: () => Answer[],
    onChanged: (newAnswers: Answer[]) => void,
}

export const SingleChoiceQuestionComponent = observer((props: SingleChoiceQuestionComponentProps) => {
    const { question } = props;
    switch(true) {
        case question.options.displayMode === 'radio' && !question.options.requireContext:
            return (<RadioSingleChoiceQuestionComponent {...props}/>);
        case question.options.displayMode === 'radio' && question.options.requireContext:
            return (<RadioSingleChoiceQuestionWithCtxComponent {...props}/>);
    }
    return (<div>Not implemented</div>);
})

const RadioSingleChoiceQuestionComponent = observer((props: SingleChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'radio') {
        return null;
    }
    const selfOnChange = (answerId: number, checked: boolean) => {
        if (checked) {
            onChanged([{ answer: [answerId, answerId], isСreatedByUser: true }])
        }        
    }

    return (
        <div>
            <div className="mb-3">
                <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            </div>
            <div className="d-flex flex-column">                
                {question.answers.map((a, idx) => 
                    <label htmlFor={`question_${question.questionId}_answer_${a.id}`} 
                           className={`comp-ph-singlechoice-label d-flex flex-row ${idx !== question.answers.length - 1 && 'mb-3' || ''}`}>
                        <div className="mr-2 mt-1">
                            <input id={`question_${question.questionId}_answer_${a.id}`} 
                                   name={`switch_${question.questionId}`} 
                                   type="radio" 
                                   checked={getAnswers().some(h => h.answer[0] === a.id)}
                                   onChange={(e) => selfOnChange(a.id, e.target.checked)} 
                                   readOnly={true} />
                        </div>
                        <div>{a.text}</div>                        
                    </label>)}
            </div>
        </div>
    );
})


const RadioSingleChoiceQuestionWithCtxComponent = observer((props: SingleChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'radio') {
        return null;
    }
    const { options } = question;
    const selfOnChange = (answerId: number, checked: boolean) => {
        if (checked) {
            onChanged([{ answer: [answerId, answerId], isСreatedByUser: true }])
        }        
    }

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`).forEach(e => {
            const id = e.id?.split(`question_${question.questionId}_answer_`)[1] ?? -1;
            const component = (<label htmlFor={`question_${question.questionId}_answer_${id}`}
                                      className={"comp-ph-singlechoice-label"}>
                                 <input id={`question_${question.questionId}_answer_${id}`} 
                                        name={`switch_${question.questionId}`} 
                                        type="radio" 
                                        checked={getAnswers().some(h => h.answer[0] === +id)}
                                        onChange={(e) => selfOnChange(+id, e.target.checked)} 
                                        readOnly={true} />
                                 <span dangerouslySetInnerHTML={{ __html: e.innerHTML }} />
                               </label>)
            ReactDOM.render(component, e);
            e.id = "";   
        })
    }, [question.questionId]);

    // apply history changes
    useEffect(() => {
        // drop all changes
        document.querySelectorAll(`input[id^="question_${question.questionId}_answer_"]`).forEach((e: any) => {
            e.checked = undefined;
        });

        // apply history changes    
        getAnswers().forEach(({ answer }) => {
            const id = answer[0];
            const answr: any = document.getElementById(`question_${question.questionId}_answer_${id}`);
            if (!answr) {
                return;
            }
            setTimeout(() => answr.checked = true, 10)
        });
    }, [question.questionId, getAnswers()])

    return (
        <div>
            <p>
                <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>            
        </div>
    );
})
