import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { ListGroup } from "react-bootstrap";
import { ExerciseController, IExerciseController } from "../controllers/exercise/exercise-controller";
import { container } from "tsyringe";
import * as E from "fp-ts/lib/Either";

export const ExercisesList = observer(() => {
    const [data, setData] = useState<number[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    useEffect(() => {
        (async() => {
            setIsLoading(true);
            const controller = container.resolve<IExerciseController>(ExerciseController);
            const dataEither = await controller.getExercises();
            if (E.isRight(dataEither)) {
                setData(dataEither.right);
            }
            setIsLoading(false);
        })();
    }, []);

    return(
        <div>
            <ListGroup>
                {data.map(i => <ListGroup.Item><a href={`exercise?exerciseId=${i}`}>{i}</a></ListGroup.Item>)}
            </ListGroup>
        </div>
    )
})
