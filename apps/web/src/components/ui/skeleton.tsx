
import type { HTMLAttributes } from "react";

type SkeletonProps = HTMLAttributes<HTMLDivElement> & {
    width?: string | number;
    height?: string | number;
    rounded?: boolean;
};

export function Skeleton({
                             width = "100%",
                             height = 16,
                             rounded = true,
                             className = "",
                             style,
                             ...props
                         }: SkeletonProps) {
    return (
        <div
            {...props}
            aria-hidden="true"
            className={`skeleton ${rounded ? "" : "skeleton--square"} ${className}`.trim()}
            style={{
                width,
                height,
                ...style,
            }}
        />
    );
}

export function MetricSkeleton() {
    return (
        <div className="panel metric-card" aria-hidden="true">
            <Skeleton width="60%" height={13} />
            <Skeleton width="42%" height={29} style={{ marginTop: 19 }} />
            <Skeleton width="78%" height={11} style={{ marginTop: 9 }} />
        </div>
    );
}
