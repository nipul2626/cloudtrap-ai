import type { ReactNode } from "react";

export type TableColumn<T> = {
  key: string;
  header: string;
  render?: (row: T) => ReactNode;
  className?: string;
  headerClassName?: string;
  align?: "start" | "center" | "end";
};

type DataTableProps<T> = {
  columns: TableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  caption: string;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyAction?: ReactNode;
  loading?: boolean;
  loadingRows?: number;
};

export function DataTable<T,>({
  columns,
  rows,
  rowKey,
  caption,
  emptyTitle = "No records found",
  emptyDescription = "Records will appear here when data is available.",
  emptyAction,
  loading = false,
  loadingRows = 5,
}: DataTableProps<T>) {
  const safeLoadingRows = Math.max(1, Math.min(loadingRows, 10));

  return (
    <div className="data-table__container">
      <table className="data-table">
        <caption className="sr-only">{caption}</caption>

        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={[
                  column.align ? `data-table__cell--${column.align}` : "",
                  column.headerClassName ?? "",
                ].filter(Boolean).join(" ")}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>

        <tbody aria-busy={loading}>
          {loading ? (
            Array.from({ length: safeLoadingRows }, (_, rowIndex) => (
              <tr key={`loading-${rowIndex}`} className="data-table__loading-row">
                {columns.map((column) => (
                  <td key={column.key}>
                    <span className="data-table__skeleton" aria-hidden="true" />
                  </td>
                ))}
              </tr>
            ))
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={Math.max(columns.length, 1)}>
                <div className="data-table__empty" role="status">
                  <strong>{emptyTitle}</strong>
                  <p>{emptyDescription}</p>
                  {emptyAction}
                </div>
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)}>
                {columns.map((column) => (
                  <td
                    key={column.key}
                    className={[
                      column.align ? `data-table__cell--${column.align}` : "",
                      column.className ?? "",
                    ].filter(Boolean).join(" ")}
                  >
                    {column.render
                      ? column.render(row)
                      : String(
                          (row as Record<string, unknown>)[column.key] ?? "—",
                        )}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
