import { observer } from "mobx-react";
import React from "react";
import { Form } from "react-bootstrap";
import { SettingValues } from "../../types/exercise-options";
import { SettingField } from "../../types/exercise-settings";

type SettingsFormProps = {
    /** Distinguishes the controls of this form from those of other forms on the page. */
    id: string,
    fields: SettingField[],
    values: SettingValues,
    defaults: SettingValues,
    path?: string[],
    onChange: (path: string[], value: boolean | number | string) => void,
}

/**
 * Settings rendered from their description. A value missing in the exercise is shown as the default:
 * the exercise may have been saved before the setting appeared.
 */
export const SettingsForm = observer(({ id, fields, values, defaults, path = [], onChange }: SettingsFormProps) => (
    <>
        {fields.map(field => {
            const fieldPath = [...path, field.name];
            const fieldId = `${id}-${fieldPath.join('-')}`;
            const value = values[field.name] ?? defaults[field.name];
            switch (field.kind) {
                case 'FLAG':
                    return <Form.Check key={field.name} type="checkbox" id={fieldId} label={field.label}
                                       checked={value === true}
                                       onChange={e => onChange(fieldPath, e.target.checked)} />;
                case 'NUMERIC':
                    return (
                        <Form.Group key={field.name} className="mb-2" controlId={fieldId}>
                            <Form.Label>{field.label}</Form.Label>
                            <Form.Control type="number" min={field.min} max={field.max}
                                          value={typeof value === 'number' ? value : ''}
                                          onChange={e => onChange(fieldPath, +e.target.value)} />
                        </Form.Group>
                    );
                case 'CHOICE':
                    return (
                        <Form.Group key={field.name} className="mb-2" controlId={fieldId}>
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
                            <SettingsForm id={id} fields={field.fields}
                                          values={isGroup(values[field.name]) ? values[field.name] as SettingValues : {}}
                                          defaults={isGroup(defaults[field.name]) ? defaults[field.name] as SettingValues : {}}
                                          path={fieldPath} onChange={onChange} />
                        </fieldset>
                    );
            }
        })}
    </>
));

function isGroup(value: SettingValues[string] | undefined): boolean {
    return typeof value === 'object';
}
