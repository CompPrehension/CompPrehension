import { hydrate } from 'react-dom'
import React from 'react';
import "./styles/index.css";
import 'bootstrap/dist/css/bootstrap.min.css';
import { Route, BrowserRouter as Router, Switch } from 'react-router-dom';
import { Assessment } from './pages/assessment';
import { Statistics } from './pages/statistics';

const Home = () => (
    <Router>
        <Switch>
            <Route exact path="/**/">
                <Assessment />
            </Route>   
            <Route path="/**/pages/statistics">
                <Statistics />
            </Route>                     
        </Switch>
    </Router>
)

hydrate(<Home />, document.getElementById('root'))
