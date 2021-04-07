import { observer } from "mobx-react"
import React, { useEffect, useState } from "react"
import { Api } from "../api";
import store from '../store';
import * as E from "fp-ts/lib/Either";
import { ExerciseStatisticsItem } from "../types/exercise-statistics";

export const Statistics = () => {
    const [statistics, setStatistics] = useState([] as ExerciseStatisticsItem[]);
    useEffect(() => {
        (async () => {
            const statistics = await Api.getExerciseStatistics(store.sessionInfo?.exerciseId ?? -1);
            if (E.isRight(statistics)) {
                setStatistics(statistics.right);
            }
        })()
    }, [statistics.length === 0])


    return (
        <div>
            <table className="table">
                <thead>
                    <tr>
                        <th scope="col">AttemptId</th>
                        <th scope="col">QuestionsCount</th>
                        <th scope="col">TotalInteractionsCount</th>
                        <th scope="col">TotalInteractionsWithErrorsCount</th>
                        <th scope="col">AverageGrade</th>
                    </tr>
                </thead>
                <tbody>
                    {statistics.map(s => (
                        <tr>
                            <th scope="row">{s.attemptId}</th>
                            <td>{s.questionsCount}</td>
                            <td>{s.totalInteractionsCount}</td>
                            <td>{s.totalInteractionsWithErrorsCount}</td>
                            <td>{s.averageGrade}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};
