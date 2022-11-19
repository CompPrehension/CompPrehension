import { observer } from "mobx-react";
import React, { useState } from "react";

const titleCase = (str: string) =>
  str.split(/\s+/).map(w => w[0].toUpperCase() + w.slice(1)).join(' ');

const ClickableLabel = ({ id, title, onChange, isChecked, style }: {id: string, title: string, onChange: (s: string) => void, isChecked: boolean, style?: React.CSSProperties}) =>
  <label htmlFor={id} onClick={() => onChange(title)} style={isChecked && style || undefined}>
    {titleCase(title)}
  </label>;

const ConcealedRadio = ({ id, value, name, selected }: { id: string, value: string, name: string, selected: string }) =>
  <input id={id} type="radio" name={name} checked={selected === value} readOnly={true} />;

export type ToggleSwitchProps<T extends string> = {
    id: string,
    values: T[],
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
          <span>
            <ConcealedRadio id={`${props.id}_${val}_checkbox`} name={`${props.id}_switch`} value={val} selected={props.selected} />
            <ClickableLabel 
              id={`${props.id}_${val}_checkbox`} 
              isChecked={val === props.selected}
              title={val}
              onChange={handleChange as (s: string) => void} 
              style={props.valueStyles?.[i] ?? undefined} />
          </span>
        );
      })}
    </div>
  );
}

