import { observer } from "mobx-react";
import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import { SingleChoiceQuestion } from "../../../types/question";

type SingleChoiceQuestionComponentProps = {
    question: SingleChoiceQuestion,
    answers: [number, number][],
    getAnswers: () => [number, number][],
    onChanged: (newAnswers: [number, number][]) => void,
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
            onChanged([[answerId, answerId]])
        }        
    }

    return (
        <div>
            <p>
                <div className="comp-ph-question-text" dangerouslySetInnerHTML={{ __html: question.text }} />
            </p>
            <p className="d-flex flex-column">                
                {question.answers.map(a => 
                    <label htmlFor={`question_${question.questionId}_answer_${a.id}`} className="d-flex flex-row mb-3">
                        <div className="mr-2 mt-1">
                            <input id={`question_${question.questionId}_answer_${a.id}`} 
                                   name={`switch_${question.questionId}`} 
                                   type="radio" 
                                   checked={getAnswers().some(h => h[0] === a.id)}
                                   onChange={(e) => selfOnChange(a.id, e.target.checked)} />
                        </div>
                        <div>{a.text}</div>                        
                    </label>)}
            </p>
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
            onChanged([[answerId, answerId]])
        }        
    }

    // on First Render 
    useEffect(() => {    
        // add button click event handlers
        document.querySelectorAll(`[id^="question_${question.questionId}_answer_"]`).forEach(e => {
            const id = e.id?.split(`question_${question.questionId}_answer_`)[1] ?? -1;
            const component = (<label htmlFor={`question_${question.questionId}_answer_${id}`}>
                                 <input id={`question_${question.questionId}_answer_${id}`} 
                                        name={`switch_${question.questionId}`} 
                                        type="radio" 
                                        checked={getAnswers().some(h => h[0] === +id)}
                                        onChange={(e) => selfOnChange(+id, e.target.checked)} />
                                 <span dangerouslySetInnerHTML={{ __html: e.innerHTML }}/>
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
        getAnswers().forEach(([id]) => {
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
