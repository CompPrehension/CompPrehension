import { observer } from "mobx-react";
import React from "react";
import { Form } from "react-bootstrap";
import { StrategySettingValues } from "../../types/exercise-options";
import { StrategySettingField } from "../../types/exercise-settings";

type StrategySettingsFormProps = {
    fields: StrategySettingField[],
    values: StrategySettingValues,
    defaults: StrategySettingValues,
    path?: string[],
    onChange: (path: string[], value: boolean | number | string) => void,
}

/**
 * Settings that the exercise strategy declares, rendered from their description. A value missing in the exercise
 * is shown as the strategy default: the exercise may have been saved before the strategy got the setting.
 */
export const StrategySettingsForm = observer(({ fields, values, defaults, path = [], onChange }: StrategySettingsFormProps) => (
    <>
        {fields.map(field => {
            const fieldPath = [...path, field.name];
            const id = `strategy-setting-${fieldPath.join('-')}`;
            const value = values[field.name] ?? defaults[field.name];
            switch (field.kind) {
                case 'FLAG':
                    return <Form.Check key={field.name} type="checkbox" id={id} label={field.label}
                                       checked={value === true}
                                       onChange={e => onChange(fieldPath, e.target.checked)} />;
                case 'NUMERIC':
                    return (
                        <Form.Group key={field.name} className="mb-2" controlId={id}>
                            <Form.Label>{field.label}</Form.Label>
                            <Form.Control type="number" min={field.min} max={field.max}
                                          value={typeof value === 'number' ? value : ''}
                                          onChange={e => onChange(fieldPath, +e.target.value)} />
                        </Form.Group>
                    );
                case 'CHOICE':
                    return (
                        <Form.Group key={field.name} className="mb-2" controlId={id}>
                            <Form.Label>{field.label}</Form.Label>
                            <Form.Select value={typeof value === 'string' ? value : ''}
                                         onChange={e => onChange(fieldPath, e.target.value)}>
                                {field.options.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
                            </Form.Select>
                        </Form.Group>
                    );
                case 'GROUP':
                    return (
                        <fieldset key={field.name} className="mb-2">
                            <legend className="fs-6 mb-1">{field.label}</legend>
                            <StrategySettingsForm fields={field.fields}
                                                  values={isGroup(values[field.name]) ? values[field.name] as StrategySettingValues : {}}
                                                  defaults={isGroup(defaults[field.name]) ? defaults[field.name] as StrategySettingValues : {}}
                                                  path={fieldPath} onChange={onChange} />
                        </fieldset>
                    );
            }
        })}
    </>
));

function isGroup(value: StrategySettingValues[string] | undefined): boolean {
    return typeof value === 'object';
}
