
"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import "./scan-results.css";

import { AppShell } from "@/components/layout/app-shell";
import { Badge, SeverityBadge, StatusBadge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { EmptyState, Panel, PanelHeading } from "@/components/ui/panel";
import { Skeleton } from "@/components/ui/skeleton";
import { getScan, getScanFindings } from "@/lib/api-client";
import type { Finding, Scan } from "@/lib/api-types";

type LoadState = "loading" | "ready" | "error";

function formatDate(value?: string | null): string {
    if (!value) return "Not available";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return "Not available";

    return new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date);
}

function formatStatus(status: string): string {
    return status.toLowerCase().replaceAll("_", " ");
}

function severityForBadge(
    severity: string,
): "critical" | "high" | "medium" | "low" | "info" {
    switch (severity.toLowerCase()) {
        case "critical":
            return "critical";
        case "high":
            return "high";
        case "medium":
            return "medium";
        case "low":
            return "low";
        default:
            return "info";
    }
}

function ScanResultsContent() {
    const params = useParams<{ scanId: string }>();
    const searchParams = useSearchParams();

    const scanId = params.scanId;
    const workspaceId = searchParams.get("workspaceId") ?? "";

    const [scan, setScan] = useState<Scan | null>(null);
    const [findings, setFindings] = useState<Finding[]>([]);
    const [totalFindings, setTotalFindings] = useState(0);
    const [state, setState] = useState<LoadState>("loading");
    const [error, setError] = useState("");
    const [refreshing, setRefreshing] = useState(false);

    const loadResults = useCallback(
        async (isRefresh = false) => {
            if (!scanId || !workspaceId) {
                setError(
                    "A workspace ID is required to retrieve this scan. Return to the scan launcher and start a scan from a workspace.",
                );
                setState("error");
                return;
            }

            if (isRefresh) {
                setRefreshing(true);
            } else {
                setState("loading");
            }

            setError("");

            try {
                const [scanResult, findingsResult] = await Promise.all([
                    getScan(scanId, workspaceId),
                    getScanFindings(scanId, workspaceId, 0, 50),
                ]);

                setScan(scanResult);
                setFindings(findingsResult.items);
                setTotalFindings(findingsResult.total);
                setState("ready");
            } catch (cause) {
                setError(
                    cause instanceof Error
                        ? cause.message
                        : "Unable to load scan results.",
                );

                if (!isRefresh) {
                    setState("error");
                }
            } finally {
                setRefreshing(false);
            }
        },
        [scanId, workspaceId],
    );

    useEffect(() => {
        const timer = window.setTimeout(() => {
            void loadResults();
        }, 0);

        return () => window.clearTimeout(timer);
    }, [loadResults]);

    const status = scan?.status?.toLowerCase() ?? "";
    const isActive = status === "queued" || status === "running";

    const recognizedStatus =
        status === "queued" ||
        status === "running" ||
        status === "completed" ||
        status === "failed";

    return (
        <AppShell>
            <main className="page-content">
                <div className="scan-results">
                    <div className="scan-results__back">
                        <Link href="/scans/new">
                            <Icon name="arrow" size={14} />
                            Back to scan launcher
                        </Link>
                    </div>

                    <header className="scan-results__heading">
                        <div>
                            <p className="scan-results__eyebrow">
                                Cloud security / Scan results
                            </p>
                            <h1>Scan results</h1>
                            <p className="scan-results__description">
                                Review execution status, scanned resources, and
                                findings reported by the CloudTrap API.
                            </p>
                        </div>

                        {scan && (
                            recognizedStatus ? (
                                <StatusBadge
                                    status={
                                        status as
                                            | "queued"
                                            | "running"
                                            | "completed"
                                            | "failed"
                                    }
                                />
                            ) : (
                                <Badge tone="neutral" dot>
                                    {formatStatus(scan.status)}
                                </Badge>
                            )
                        )}
                    </header>

                    {state === "loading" ? (
                        <Panel>
                            <PanelHeading
                                title="Loading scan results"
                                description="Retrieving scan status and findings."
                            />
                            <div className="scan-results__skeletons">
                                <Skeleton height={76} />
                                <Skeleton height={76} />
                                <Skeleton height={76} />
                            </div>
                        </Panel>
                    ) : state === "error" ? (
                        <Panel>
                            <EmptyState
                                icon="alert"
                                title="Scan results unavailable"
                                description={error}
                                action={
                                    <Button
                                        variant="secondary"
                                        onClick={() => void loadResults()}
                                        icon="activity"
                                    >
                                        Retry
                                    </Button>
                                }
                            />
                        </Panel>
                    ) : scan ? (
                        <>
                            {error && (
                                <div
                                    className="scan-results__notice"
                                    role="alert"
                                >
                                    Latest refresh failed: {error}
                                </div>
                            )}

                            <section
                                className="scan-results__metrics"
                                aria-label="Scan summary"
                            >
                                <Panel className="scan-results__metric">
                                    <span>Assets discovered</span>
                                    <strong>
                                        {scan.assets_discovered ?? "—"}
                                    </strong>
                                    <small>Reported by the scan</small>
                                </Panel>

                                <Panel className="scan-results__metric">
                                    <span>Assets scanned</span>
                                    <strong>
                                        {scan.assets_scanned ?? "—"}
                                    </strong>
                                    <small>Reported by the scan</small>
                                </Panel>

                                <Panel className="scan-results__metric">
                                    <span>Findings created</span>
                                    <strong>
                                        {scan.findings_created ?? totalFindings}
                                    </strong>
                                    <small>Scan summary</small>
                                </Panel>
                            </section>

                            <Panel>
                                <PanelHeading
                                    title="Execution details"
                                    description="Identifiers and timestamps returned by the backend."
                                    action={
                                        <Button
                                            variant="secondary"
                                            onClick={() =>
                                                void loadResults(true)
                                            }
                                            loading={refreshing}
                                            icon="activity"
                                        >
                                            Refresh
                                        </Button>
                                    }
                                />

                                <dl className="scan-results__details">
                                    <div>
                                        <dt>Scan ID</dt>
                                        <dd>{scan.id}</dd>
                                    </div>
                                    <div>
                                        <dt>Workspace ID</dt>
                                        <dd>{scan.workspace_id}</dd>
                                    </div>
                                    <div>
                                        <dt>Dataset</dt>
                                        <dd>
                                            {scan.dataset_id ?? "Not provided"}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Source</dt>
                                        <dd>
                                            {scan.source || "Not provided"}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Queued</dt>
                                        <dd>{formatDate(scan.queued_at)}</dd>
                                    </div>
                                    <div>
                                        <dt>Started</dt>
                                        <dd>{formatDate(scan.started_at)}</dd>
                                    </div>
                                    <div>
                                        <dt>Completed</dt>
                                        <dd>{formatDate(scan.completed_at)}</dd>
                                    </div>
                                </dl>

                                {isActive && (
                                    <div className="scan-results__notice">
                                        This scan is still{" "}
                                        {formatStatus(scan.status)}. Refresh to
                                        check for updated results.
                                    </div>
                                )}

                                {status === "failed" && (
                                    <div
                                        className="scan-results__notice scan-results__notice--error"
                                        role="alert"
                                    >
                                        <strong>Scan failed</strong>
                                        <p>
                                            {scan.safe_error_message ||
                                                "The backend did not provide a safe error message."}
                                        </p>
                                        {scan.error_code && (
                                            <p>
                                                Error code: {scan.error_code}
                                            </p>
                                        )}
                                    </div>
                                )}
                            </Panel>

                            <Panel>
                                <PanelHeading
                                    title="Findings"
                                    description={`${totalFindings} total findings reported for this scan. Showing ${findings.length} records from the current page.`}
                                    action={
                                        <Badge tone="neutral">
                                            {findings.length} loaded
                                        </Badge>
                                    }
                                />

                                {findings.length === 0 ? (
                                    <EmptyState
                                        icon="shield"
                                        title="No findings returned"
                                        description={
                                            isActive
                                                ? "Findings may become available as the scan progresses."
                                                : "The API returned no findings for this scan."
                                        }
                                    />
                                ) : (
                                    <div className="scan-results__table-wrap">
                                        <table className="scan-results__table">
                                            <thead>
                                            <tr>
                                                <th>Finding</th>
                                                <th>Severity</th>
                                                <th>Status</th>
                                                <th>Risk score</th>
                                                <th>Last detected</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {findings.map((finding) => (
                                                <tr key={finding.id}>
                                                    <td>
                                                        <div className="scan-results__finding">
                                                            <strong>
                                                                {finding.title}
                                                            </strong>
                                                            {finding.description && (
                                                                <small>
                                                                    {finding.description}
                                                                </small>
                                                            )}
                                                            <small>
                                                                ID: {finding.id}
                                                            </small>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <SeverityBadge
                                                            severity={severityForBadge(
                                                                finding.severity,
                                                            )}
                                                        />
                                                    </td>
                                                    <td>
                                                        <Badge tone="neutral">
                                                            {formatStatus(
                                                                finding.status,
                                                            )}
                                                        </Badge>
                                                    </td>
                                                    <td>
                                                        {finding.risk_score ??
                                                            "Not scored"}
                                                    </td>
                                                    <td>
                                                        {formatDate(
                                                            finding.last_detected_at,
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </Panel>
                        </>
                    ) : null}
                </div>
            </main>
        </AppShell>
    );
}

export default function ScanResultsPage() {
    return (
        <Suspense
            fallback={
                <main className="page-content">
                    <div className="scan-results">
                        <Panel>
                            <PanelHeading
                                title="Loading scan results"
                                description="Preparing the scan details page."
                            />
                        </Panel>
                    </div>
                </main>
            }
        >
            <ScanResultsContent />
        </Suspense>
    );
}
