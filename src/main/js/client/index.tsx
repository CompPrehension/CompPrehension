import { observer } from 'mobx-react-lite'
import { useEffect } from 'react';
import { hydrate } from 'react-dom'
import store from './store';
import { QuestionFabric } from './components/question/question-fabric';
import React from 'react';
import { CircularProgress } from '@material-ui/core';
import "./styles/index.css"
import {UserInfo} from "./components/user-info";
import { NextQuestionBtn } from './components/next-question-btn';

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
        return <CircularProgress />;
    }

    return (
        <div>
            <UserInfo />
            <QuestionFabric />
            <NextQuestionBtn />
        </div>
    );
})

hydrate(<Home />, document.getElementById('root'))