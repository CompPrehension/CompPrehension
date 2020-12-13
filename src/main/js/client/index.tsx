import { observer } from 'mobx-react-lite'
import { useEffect } from 'react';
import { hydrate } from 'react-dom'
import store from './store';
import { QuestionFabric } from './question/question-fabric';
import React from 'react';
import { CircularProgress } from '@material-ui/core';
import "./styles/index.css"

const Home = observer(() => {
    useEffect(() => {
        (async () => {
            await store.createSession();
            await store.loadQuestion();
        })()
    }, []);

    if (store.isLoading) {
        return <CircularProgress />;
    }

    return (
        <div>
            <QuestionFabric />
        </div>
    );
})

hydrate(<Home />, document.getElementById('root'))