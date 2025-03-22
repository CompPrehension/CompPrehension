import { observer } from "mobx-react";
import React, { useState } from "react";
import { ToggleSwitch } from "../components/common/toggle";

export const db = {
    concepts: [
        'literal',
        'variable',
        'arithmetic operator',
        'assignment operator',
        'comparison operator',
        'logical operator',
        'bitwise operator',
        'array access operator',
        'pointer operator',
        //'object access operator',
        'function call',
        //'conditional operator',
        //'type cast operator',
        //'increment/decrement operator',
    ],
    laws: [
        'Expression contains two operators with different precedence in a row',
        'Expression contains two operators with the same precedence and left associativity in a row',
        'Expression contains two operators with the same precedence and right associativity in a row',
        'Expression contains operators inside parentheses or another operator',
        'Expression contains operators evaluating their operands in a strict order',
        //"Expression contains operator(s) that aren't evaluated.",
        //'Student finishes expression evaluation too soon without evaluating everything.',
    ]
}


export const StrategySettings = observer(() => {

    const multiselectCss = {
        'width': '200px',
    }
    const buttonCss = {
        border: '1.5px solid rgb(182 182 182)',
        borderRadius: '5px',
    }


    return (
        <div className="container-fluid" style={{ color: 'black', fontSize: '18px'}}>
            <div className="row">
                <div className="col-md-8">
                    <form>
                        <div className="form-group">
                            <label className="font-weight-bold">Domain</label>
                            <select className="form-control">
                                <option>Order of Expression Evaluation</option>
                            </select>
                        </div>
                        <div className="row">
                            <div className="col-md-6">
                                <div className="form-group">
                                    <label className="font-weight-bold">Question complexity</label>
                                    <div>
                                        <input type="range" className="form-control-range" id="formControlRange1" />
                                        {/*
                                            <div className="form-check form-check-inline" >
                                                <input className="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio1" value="option1" checked={true} />
                                                <label className="form-check-label" htmlFor="inlineRadio1">low</label>
                                            </div>
                                            <div className="form-check form-check-inline">
                                                <input className="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio2" value="option2" />
                                                <label className="form-check-label" htmlFor="inlineRadio2">medium</label>
                                            </div>
                                            <div className="form-check form-check-inline">
                                                <input className="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio3" value="option3" />
                                                <label className="form-check-label" htmlFor="inlineRadio3">high</label>
                                            </div>
                                        */}
                                        
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="form-group">
                                    <label className="font-weight-bold">Answer length</label>
                                    <div>
                                        <input type="range" className="form-control-range" id="formControlRange1" />
                                        
                                        {/**
                                            <div className="form-check form-check-inline">
                                            <input className="form-check-input" type="radio" name="test" id="_inlineRadio1" value="option1" />
                                            <label className="form-check-label" htmlFor="_inlineRadio1">low</label>
                                        </div>
                                        <div className="form-check form-check-inline">
                                            <input className="form-check-input" type="radio" name="test" id="_inlineRadio2" value="option2" checked={true} />
                                            <label className="form-check-label" htmlFor="_inlineRadio2">medium</label>
                                        </div>
                                        <div className="form-check form-check-inline">
                                            <input className="form-check-input" type="radio" name="test" id="_inlineRadio3" value="option3" />
                                            <label className="form-check-label" htmlFor="_inlineRadio3">high</label>
                                        </div>
                                    */}
                                    </div>

                                </div>
                            </div>
                        </div>





                        <div className="form-group">
                            <div>
                                <label className="font-weight-bold">Concepts</label>
                            </div>

                            <div className="row">
                                <div className="col-md-6">
                                    {db.concepts.filter((_, i) => i % 2 === 0).map((c, idx) =>
                                        <div className="d-flex flex-row align-items-center" style={{ marginBottom: '10px' }}>
                                            <ToggleSwitch id={`concept_toggle_${c}_${idx}`}
                                                selected={'Allowed'}
                                                values={['Denied', 'Allowed', 'Target']}
                                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                onChange={val => 0} />
                                            <div style={{ marginLeft: '15px' }}>{c}</div>
                                        </div>)}
                                </div>
                                <div className="col-md-6">
                                    {db.concepts.filter((_, i) => i % 2 !== 0).map((c, idx) =>
                                        <div className="d-flex flex-row align-items-center" style={{ marginBottom: '10px' }}>
                                            <ToggleSwitch id={`concept_toggle_${c}_${idx}`}
                                                selected={'Allowed'}
                                                values={['Denied', 'Allowed', 'Target']}
                                                valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                                onChange={val => 0} />
                                            <div style={{ marginLeft: '15px' }}>{c}</div>
                                        </div>)}
                                </div>

                            </div>
                            {/* <div className="font-weight-bold text-justify" style={{ marginTop: '-10px', fontSize: '19px', letterSpacing: '3px' }}>...</div> */}

                        </div>
                        <div className="form-group">
                            <label className="font-weight-bold">Laws</label>
                            {db.laws.map((c, idx) =>
                            (<div className="d-flex flex-row align-items-start justify-content-start" style={{ marginBottom: '10px' }}>
                                <div>
                                    <ToggleSwitch id={`law_toggle_${idx}`}
                                        selected={'Allowed'}
                                        values={['Denied', 'Allowed', 'Target']}
                                        valueStyles={[{ backgroundColor: '#eb2828' }, null, { backgroundColor: '#009700' }]}
                                        onChange={val => 0} />
                                </div>
                                <div style={{ marginLeft: '15px' }}>{c}</div>
                            </div>))}
                            {/* <div className="font-weight-bold text-justify" style={{ marginTop: '-10px', fontSize: '19px', letterSpacing: '3px' }}>...</div> */}

                        </div>


                    </form>
                </div>

            </div>
        </div>
    );
})
