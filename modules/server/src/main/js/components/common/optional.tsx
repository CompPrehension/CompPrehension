import { observer } from "mobx-react";
import React from "react";


export const Optional = observer((props: { isVisible: boolean, children?: React.ReactNode[] | React.ReactNode }) => {
    const { isVisible, children } = props;
    if (!isVisible) {
        return null;        
    }
    return (<>{children}</>);
})
