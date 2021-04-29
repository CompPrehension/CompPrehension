import { hydrate } from 'react-dom'
import React from 'react';
import "./bootstrapper";
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Route, BrowserRouter as Router, Switch } from 'react-router-dom';
import { Exercise } from './pages/exercise';
import { Statistics } from './pages/statistics';

const Home = () => (
    <div className="container comp-ph-container">
        <Router>
            <Switch>
                <Route path="/**/pages/statistics">
                    <Statistics />
                </Route>
                <Route exact path="/**/">
                    <Exercise />
                </Route>             
            </Switch>
        </Router>
    </div>
)

hydrate(<Home />, document.getElementById('root'))
