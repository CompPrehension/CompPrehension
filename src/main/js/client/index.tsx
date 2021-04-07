import { hydrate } from 'react-dom'
import React from 'react';
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Route, BrowserRouter as Router, Switch } from 'react-router-dom';
import { Assessment } from './pages/assessment';
import { Statistics } from './pages/statistics';

const Home = () => (
    <div className="container comp-ph-container">
        <Router>
            <Switch>
                <Route path="/**/pages/statistics">
                    <Statistics />
                </Route>
                <Route exact path="/**/">
                    <Assessment />
                </Route>             
            </Switch>
        </Router>
    </div>
)

hydrate(<Home />, document.getElementById('root'))
