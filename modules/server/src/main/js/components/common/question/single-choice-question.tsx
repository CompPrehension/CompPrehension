import parse, { domToReact, Element } from "html-react-parser";
import { answerSlotId } from "./answer-slot";
import { observer } from "mobx-react";
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
            onChanged([{ answer: [answerId, answerId], isCreatedByUser: true }])
        }
    }

    return (
      <div>
        <div className='mb-3'>
          <div
            className='comp-ph-question-text'
            dangerouslySetInnerHTML={{ __html: question.text }}
          />
        </div>
        <div className='d-flex flex-column'>
          {question.answers.map((a, idx) => (
            <label
              key={a.id}
              htmlFor={`question_${question.questionId}_answer_${a.id}`}
              className={`comp-ph-singlechoice-label d-flex flex-row ${
                (idx !== question.answers.length - 1 && 'mb-3') || ''
              }`}
            >
              <div className='mr-2 mt-1'>
                <input
                  id={`question_${question.questionId}_answer_${a.id}`}
                  name={`switch_${question.questionId}`}
                  type='radio'
                  checked={getAnswers().some((h) => h.answer[0] === a.id)}
                  onChange={(e) => selfOnChange(a.id, e.target.checked)}
                  readOnly={true}
                />
              </div>
              <div dangerouslySetInnerHTML={{ __html: a.text }} />
            </label>
          ))}
        </div>
      </div>
    );
})


const RadioSingleChoiceQuestionWithCtxComponent = observer((props: SingleChoiceQuestionComponentProps) => {
    const { question, getAnswers, onChanged } = props;
    if (question.options.displayMode !== 'radio') {
        return null;
    }
    const selfOnChange = (answerId: number, checked: boolean) => {
        if (checked) {
            onChanged([{ answer: [answerId, answerId], isCreatedByUser: true }])
        }
    }

    const content = parse(question.text, {
        replace: (node) => {
            const id = answerSlotId(node);
            if (id === null) {
                return;
            }

            return (
                <label htmlFor={`question_${question.questionId}_answer_${id}`}
                       className="comp-ph-singlechoice-label">
                    <input id={`question_${question.questionId}_answer_${id}`}
                           name={`switch_${question.questionId}`}
                           data-answer-id={id}
                           type="radio"
                           checked={getAnswers().some(h => h.answer[0] === id)}
                           onChange={(e) => selfOnChange(id, e.target.checked)}
                           readOnly={true} />
                    <span>{domToReact((node as Element).children as never)}</span>
                </label>
            );
        },
    });

    return (
        <div id={`question_${question.questionId}`}>
            <div className="comp-ph-question-text">{content}</div>
        </div>
    );
})
