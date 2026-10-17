
import type {
    ButtonHTMLAttributes,
    ReactNode,
} from "react";
import { Icon, type IconName } from "./icon";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: ButtonVariant;
    loading?: boolean;
    icon?: IconName;
    trailingIcon?: IconName;
    children: ReactNode;
};

export function Button({
                           variant = "secondary",
                           loading = false,
                           icon,
                           trailingIcon,
                           className = "",
                           children,
                           disabled,
                           ...props
                       }: ButtonProps) {
    return (
        <button
            {...props}
            className={`ui-button ui-button--${variant} ${className}`.trim()}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
        >
            {loading ? (
                <Icon name="loader" className="ui-button__spinner" />
            ) : icon ? (
                <Icon name={icon} />
            ) : null}
            <span>{children}</span>
            {!loading && trailingIcon ? <Icon name={trailingIcon} /> : null}
        </button>
    );
}

type IconButtonProps = Omit<
    ButtonHTMLAttributes<HTMLButtonElement>,
    "children"
> & {
    icon: IconName;
    label: string;
    size?: number;
};

export function IconButton({
                               icon,
                               label,
                               size = 18,
                               className = "",
                               ...props
                           }: IconButtonProps) {
    return (
        <button
            {...props}
            type={props.type ?? "button"}
            className={`ui-icon-button ${className}`.trim()}
            aria-label={label}
            title={label}
        >
            <Icon name={icon} size={size} />
        </button>
    );
}
