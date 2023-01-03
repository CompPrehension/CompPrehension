import { observer } from "mobx-react";
import React, { useState } from "react";

type ClickableLabelProps = {
  id: string, 
  title: string,
  value: string,
  onChange: (s: string) => void, 
  isChecked: boolean, 
  style?: React.CSSProperties,
}
const ClickableLabel = ({ id, title, value, onChange, isChecked, style }: ClickableLabelProps) =>
  <label htmlFor={id} onClick={() => onChange(value)} style={isChecked && style || undefined}>
    {title}
  </label>;


type ConcealedRadioProps = { 
  id: string, 
  value: string, 
  name: string, 
  selected: string,
}
const ConcealedRadio = ({ id, value, name, selected }: ConcealedRadioProps) =>
  <input id={id} type="radio" name={name} checked={selected === value} readOnly={true} />;

export type ToggleSwitchProps<T extends string> = {
    id: string,
    values: T[],
    displayNames?: string[],
    valueStyles?: (React.CSSProperties | null | undefined)[],
    selected: T,
    onChange?: (val: T) => void,
}
export const ToggleSwitch = <T extends string,>(props: ToggleSwitchProps<T>) => {
  const handleChange = (val: T) => {
    props.onChange?.(val);
  };

  const selectionStyle = () => {
    return {
      left: `${props.values.indexOf(props.selected) / 3 * 100}%`,
    };
  };

  return (
    <div id={props.id} className="switch-field d-inline-flex flex-row">
      {props.values.map((val, i) => {
        return (
          <span key={i}>
            <ConcealedRadio id={`${props.id}_${val}_checkbox`}
                            name={`${props.id}_switch`} 
                            value={val} 
                            selected={props.selected} />
            <ClickableLabel 
              id={`${props.id}_${val}_checkbox`} 
              isChecked={val === props.selected}
              title={props.displayNames?.[i] ?? val}
              value={val}
              onChange={handleChange as (s: string) => void} 
              style={props.valueStyles?.[i] ?? undefined} />
          </span>
        );
      })}
    </div>
  );
}

