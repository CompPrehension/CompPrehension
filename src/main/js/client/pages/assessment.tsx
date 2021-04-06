import { observer } from "mobx-react";
import React, { useEffect } from "react";
import { Feedback } from "../components/feedback";
import { GenerateNextQuestionBtn } from "../components/generate-next-question-btn";
import { Header } from "../components/header";
import { Loader } from "../components/loader";
import { QuestionFabric } from "../components/question/question-fabric";
import store from '../store';

export const Assessment = observer(() => {
    useEffect(() => {
        (async () => {
            await store.loadSessionInfo();
            const { questionIds=[] } = store.sessionInfo ?? {};
            if (questionIds.length > 0) {
                await store.loadQuestion(questionIds[questionIds.length - 1]);
            } else {
                await store.generateQuestion();
            }           
        })()
    }, []);

    if (store.isSessionLoading) {
        return <Loader />;
    }

    return (
        <div className="container comp-ph-container">
            <Header />
            <QuestionFabric />
            <Feedback />
            <GenerateNextQuestionBtn />            
        </div>
    );
})
