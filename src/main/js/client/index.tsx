import { observer } from 'mobx-react-lite'
import { useEffect } from 'react';
import { hydrate } from 'react-dom'
import store from './store';
import { QuestionFabric } from './components/question/question-fabric';
import React from 'react';
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { GenerateNextQuestionBtn } from './components/generate-next-question-btn';
import { Feedback } from './components/feedback';
import { Loader } from './components/loader';
import { Header } from './components/header';

const Home = observer(() => {
    useEffect(() => {
        (async () => {
            await store.loadSessionInfo();
            const { questionIds=[] } = store.sessionInfo ?? {};
            if (questionIds.length > 0) {
                await store.loadQuestion(questionIds[0]);
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

hydrate(<Home />, document.getElementById('root'))