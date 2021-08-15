import { observer } from "mobx-react"
import React, { useEffect, useState } from "react"
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import * as E from "fp-ts/lib/Either";
import { ExerciseStatisticsItem } from "../types/exercise-statistics";
import { LoadingWrapper } from "../components/common/loader";
import { container } from "tsyringe";

export const Statistics = () => {
    const [statistics, setStatistics] = useState([] as ExerciseStatisticsItem[]);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        (async () => {
            const urlParams = new URLSearchParams(window.location.search);
            const exerciseId = urlParams.get('exerciseId');
            if (exerciseId === null || Number.isNaN(+exerciseId)) {
                throw new Error("Invalid exerciseId url param");
            }

            const controller = container.resolve<IExerciseController>(ExerciseController);
            setIsLoading(true);
            const statistics = await controller.getExerciseStatistics(+exerciseId);
            if (E.isRight(statistics)) {
                setStatistics(statistics.right);
            }
            setIsLoading(false);
        })()
    }, []);


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
                    <LoadingWrapper isLoading={isLoading}>
                        {statistics.map(s => (
                            <tr>
                                <th scope="row">{s.attemptId}</th>
                                <td>{s.questionsCount}</td>
                                <td>{s.totalInteractionsCount}</td>
                                <td>{s.totalInteractionsWithErrorsCount}</td>
                                <td>{s.averageGrade}</td>
                            </tr>
                        ))}
                    </LoadingWrapper>
                </tbody>
            </table>
        </div>
    );
};
