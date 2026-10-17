import type { SelectHTMLAttributes } from "react";
import { Icon } from "./icon";

export type SelectOption = {
  value: string;
  label: string;
  disabled?: boolean;
};

type SelectFieldProps = Omit<
  SelectHTMLAttributes<HTMLSelectElement>,
  "size"
> & {
  label: string;
  options: SelectOption[];
  placeholder?: string;
  hint?: string;
  error?: string;
};

export function SelectField({
  id,
  label,
  options,
  placeholder,
  hint,
  error,
  className = "",
  required,
  ...props
}: SelectFieldProps) {
  if (!id) {
    throw new Error("SelectField requires a stable id.");
  }

  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
        {required ? <span className="field__required" aria-hidden="true"> *</span> : null}
      </label>

      <div className={`field__control field__control--select ${error ? "field__control--error" : ""}`}>
        <select
          {...props}
          id={id}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`field__select ${className}`.trim()}
        >
          {placeholder ? (
            <option value="" disabled={required}>
              {placeholder}
            </option>
          ) : null}

          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
              disabled={option.disabled}
            >
              {option.label}
            </option>
          ))}
        </select>

        <span className="field__select-icon" aria-hidden="true">
          <Icon name="chevron-down" size={16} />
        </span>
      </div>

      {hint ? <p className="field__hint" id={hintId}>{hint}</p> : null}
      {error ? <p className="field__error" id={errorId} role="alert">{error}</p> : null}
    </div>
  );
}
