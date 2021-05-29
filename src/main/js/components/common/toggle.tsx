import React from "react";

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
export class ToggleSwitch extends React.PureComponent<ToggleSwitchProps> {
    handleChange = (val: string) => {
        this.props.onChange?.(val);
    };

    selectionStyle = () => {
        return {
            left: `${this.props.values.indexOf(this.props.selected) / 3 * 100}%`,
        };
    };

    render() {
        const { selected } = this.props;
        return (
          <div id={this.props.id} className="switch-field d-inline-flex flex-row">
            {this.props.values.map(val => {
              return (
                <span>
                  <ConcealedRadio id={`${this.props.id}_${val}_checkbox`} name={`${this.props.id}_switch`} value={val} selected={selected} />
                  <ClickableLabel id={`${this.props.id}_${val}_checkbox`} title={val} onChange={this.handleChange} />
                </span>
              );
            })}
          </div>
        );
      }
}

