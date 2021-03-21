import { observer } from 'mobx-react-lite'
import { useEffect } from 'react';
import { hydrate } from 'react-dom'
import store from './store';
import { QuestionFabric } from './components/question/question-fabric';
import React from 'react';
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { NextQuestionBtn } from './components/next-question-btn';
import { Feedback } from './components/feedback';
import { Spinner } from 'react-bootstrap';
import { Loader } from './components/loader';
import { Header } from './components/header';

const Home = observer(() => {
    useEffect(() => {
        (async () => {
            await store.loadSessionInfo();
            const { attemptIds=[] } = store.sessionInfo ?? {};
            if (attemptIds.length > 0) {
                await store.loadQuestion(attemptIds[0]);
            }            
        })()
    }, []);

    if (store.isLoading) {
        return <Loader />;
    }

    return (
        <div className="container comp-ph-container">
            <Header />
            <QuestionFabric />
            <Feedback />
            <NextQuestionBtn />
        </div>
    );
})

hydrate(<Home />, document.getElementById('root'))