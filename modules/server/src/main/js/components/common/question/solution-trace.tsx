import {observer} from "mobx-react";
import React from "react";

type SolutionTraceComponentProps = {
    trace?: string[] | null,
}

export const SolutionTraceComponent = observer((props: SolutionTraceComponentProps) => {
    const { trace } = props;
    if (!trace || trace.length === 0) {
        return null;
    }
    return (
        <div>
            <table className="comp-ph-trace">
                <tbody>
                    {trace.map((t, idx) => <tr key={idx}><td dangerouslySetInnerHTML={{ __html: t }}></td></tr>)}
                </tbody>
            </table>
        </div>
    );
})
