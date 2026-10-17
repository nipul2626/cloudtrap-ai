import type { InputHTMLAttributes, ReactNode } from "react";
import { Icon, type IconName } from "./icon";

type TextFieldProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "size"
> & {
  label: string;
  hint?: string;
  error?: string;
  leadingIcon?: IconName;
  trailingContent?: ReactNode;
};

export function TextField({
  id,
  label,
  hint,
  error,
  leadingIcon,
  trailingContent,
  className = "",
  required,
  ...props
}: TextFieldProps) {
  if (!id) {
    throw new Error("TextField requires a stable id.");
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

      <div className={`field__control ${error ? "field__control--error" : ""}`}>
        {leadingIcon ? (
          <span className="field__leading-icon" aria-hidden="true">
            <Icon name={leadingIcon} size={17} />
          </span>
        ) : null}

        <input
          {...props}
          id={id}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`field__input ${leadingIcon ? "field__input--leading" : ""} ${className}`.trim()}
        />

        {trailingContent ? (
          <span className="field__trailing">{trailingContent}</span>
        ) : null}
      </div>

      {hint ? <p className="field__hint" id={hintId}>{hint}</p> : null}
      {error ? <p className="field__error" id={errorId} role="alert">{error}</p> : null}
    </div>
  );
}

type SearchFieldProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "type" | "size"
> & {
  label: string;
  onClear?: () => void;
};

export function SearchField({
  id,
  label,
  value,
  onClear,
  className = "",
  ...props
}: SearchFieldProps) {
  if (!id) {
    throw new Error("SearchField requires a stable id.");
  }

  const hasValue = typeof value === "string" && value.length > 0;

  return (
    <div className={`search-field ${className}`.trim()}>
      <label className="search-field__icon" htmlFor={id}>
        <Icon name="search" size={17} />
        <span className="sr-only">{label}</span>
      </label>

      <input
        {...props}
        id={id}
        type="search"
        value={value}
        aria-label={label}
        className="search-field__input"
      />

      {hasValue && onClear ? (
        <button
          type="button"
          className="search-field__clear"
          onClick={onClear}
          aria-label={`Clear ${label.toLowerCase()}`}
        >
          <Icon name="close" size={15} />
        </button>
      ) : null}
    </div>
  );
}
