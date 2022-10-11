import { observer } from "mobx-react";
import React from "react";


export const StrategySettings = observer(() => {

    const multiselectCss = {
        'width': '200px',
    }
    const buttonCss = {
        border: '1.5px solid rgb(182 182 182)',
        borderRadius: '5px',
    }

    return (
        <div className="container-fluid">
            <div className="flex-xl-nowrap row">
                <form>
                    <div className="form-group">
                        <label>Strategy name</label>
                        <input className="form-control" />                        
                    </div>
                    <div className="form-group">
                        <label>TargetConcepts</label>

                        <div className="row">
                            <div className="col">
                                <select style={multiselectCss} multiple className="form-control" id="exampleFormControlSelect2">
                                    <option>1</option>
                                    <option>2</option>
                                    <option>3</option>
                                    <option>4</option>
                                    <option>5</option>
                                </select>
                            </div>
                            <div className="col">
                                <button style={buttonCss}> =&gt; </button>
                            </div>
                            <div className="col">
                                <select style={multiselectCss} multiple className="form-control" id="exampleFormControlSelect3">
                                    <option>5</option>
                                    <option>6</option>
                                    
                                </select>
                            </div>
                        </div>
                    </div>
                    <div className="form-group">
                        <label>Denied Concepts</label>
                        <div className="row">
                            <div className="col">
                                <select style={multiselectCss} multiple className="form-control" id="exampleFormControlSelect2">
                                    <option>1</option>
                                    <option>2</option>
                                    <option>3</option>
                                    <option>4</option>
                                    <option>5</option>
                                </select>
                            </div>
                            <div className="col">
                                <button style={buttonCss}> =&gt; </button>
                            </div>
                            <div className="col">
                                <select style={multiselectCss} multiple className="form-control" id="exampleFormControlSelect3">
                                    <option>1</option>
                                    <option>2</option>
                                    <option>3</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
})