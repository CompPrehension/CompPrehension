import { observer } from "mobx-react";
import React, { useEffect, useState } from "react";
import { ListGroup } from "react-bootstrap";
import { LoadingWrapper } from "../components/common/loader";
import { exerciseController } from "../controllers";
import * as E from "fp-ts/lib/Either";

export const ExercisesList = observer(() => {
    const [data, setData] = useState<number[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    useEffect(() => {
        (async() => {
            setIsLoading(true);
            const dataEither = await exerciseController.getExercises();
            if (E.isRight(dataEither)) {
                setData(dataEither.right);
            }
            setIsLoading(false);
        })();
    }, []);

    return(
        <div>
            <LoadingWrapper isLoading={isLoading}>
                <ListGroup>
                    {data.map(i => <ListGroup.Item><a href={`exercise?exerciseId=${i}`}>{i}</a></ListGroup.Item>)}
                </ListGroup>
            </LoadingWrapper>
        </div>
    )
})
