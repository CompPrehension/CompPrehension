import React, { useState } from "react";

const titleCase = (str: string) =>
  str.split(/\s+/).map(w => w[0].toUpperCase() + w.slice(1)).join(' ');

const ClickableLabel = ({ id, title, onChange }: {id: string, title: string, onChange: (s: string) => void}) =>
  <label htmlFor={id} onClick={() => onChange(title)}>
    {titleCase(title)}
  </label>;

const ConcealedRadio = ({ id, value, name, selected }: { id: string, value: string, name: string, selected: string }) =>
  <input id={id} type="radio" name={name} checked={selected === value} />;

export type ToggleSwitchProps = {
    id: string,
    values: string[],
    selected: string,
    onChange?: (val: string) => void,
}

export const ToggleSwitch = (props: ToggleSwitchProps) => {
  const [selected, setSelected] = useState(props.selected)
  const handleChange = (val: string) => {
    setSelected(val);
    props.onChange?.(val);
  };

  const selectionStyle = () => {
    return {
      left: `${props.values.indexOf(props.selected) / 3 * 100}%`,
    };
  };

  return (
    <div id={props.id} className="switch-field d-inline-flex flex-row">
      {props.values.map(val => {
        return (
          <span>
            <ConcealedRadio id={`${props.id}_${val}_checkbox`} name={`${props.id}_switch`} value={val} selected={selected} />
            <ClickableLabel id={`${props.id}_${val}_checkbox`} title={val} onChange={handleChange} />
          </span>
        );
      })}
    </div>
  );
}
