import { hydrate } from 'react-dom'
import React from 'react';
import "./bootstrapper";
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Route, BrowserRouter as Router, Switch } from 'react-router-dom';
import { Exercise } from './pages/exercise';
import { Statistics } from './pages/statistics';
import { ExercisesList } from './pages/exercises-list';

const Home = () => (
    <div className="container comp-ph-container">
        <Router>
            <Switch>
                <Route path="/**/pages/statistics">ExerciseList
                    <Statistics />
                </Route>
                <Route path="/**/pages/exercise">
                    <Exercise />
                </Route>
                <Route path="/**/pages/exercises-list">
                    <ExercisesList />
                </Route>
            </Switch>
        </Router>
    </div>
)

hydrate(<Home />, document.getElementById('root'))
