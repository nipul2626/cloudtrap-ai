
"use client";
import "./scan-page.css";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Panel, PanelHeading } from "@/components/ui/panel";
import type { Workspace } from "@/lib/api-types";
import { ApiError, createDemoScan, getWorkspaces } from "@/lib/api-client";

type WorkspaceState = "loading" | "ready" | "error";

export default function NewScanPage() {
    const router = useRouter();

    const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
    const [workspaceId, setWorkspaceId] = useState("");
    const [workspaceState, setWorkspaceState] =
        useState<WorkspaceState>("loading");
    const [workspaceError, setWorkspaceError] = useState("");
    const [scanError, setScanError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        let cancelled = false;

        async function loadWorkspaces() {
            try {
                const response = await getWorkspaces();

                if (cancelled) return;

                setWorkspaces(response.content);
                setWorkspaceState("ready");
                setWorkspaceId((current) => current || response.content[0]?.id || "");
            } catch (error) {
                if (cancelled) return;

                setWorkspaceState("error");
                setWorkspaceError(
                    error instanceof Error
                        ? error.message
                        : "Unable to load workspaces.",
                );
            }
        }

        void loadWorkspaces();

        return () => {
            cancelled = true;
        };
    }, []);

    async function handleLaunch() {
        if (!workspaceId || submitting) return;

        setSubmitting(true);
        setScanError("");

        try {
            // A fresh key per launch attempt; the backend protects retries
            // that reuse the same key for the same workspace and dataset.
            const idempotencyKey = crypto.randomUUID();
            const scan = await createDemoScan(workspaceId, idempotencyKey);

            router.push(
                `/scans/${encodeURIComponent(scan.id)}?workspaceId=${encodeURIComponent(workspaceId)}`,
            );
        } catch (error) {
            const message =
                error instanceof ApiError
                    ? error.status === 401
                        ? "Your session is not authenticated. Sign in and try again."
                        : error.message
                    : error instanceof Error
                        ? error.message
                        : "The scan could not be started.";

            setScanError(message);
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <AppShell>
            <main className="page-content">
                <div className="scan-page">
                    <header className="scan-page__heading">
                        <div>
                            <p className="scan-page__eyebrow">
                                Cloud security / Scanning
                            </p>
                            <h1>Run a security scan</h1>
                            <p className="scan-page__description">
                                Start a scan against the supported demo dataset
                                and review the results returned by CloudTrap API.
                            </p>
                        </div>

                        <Badge tone="info" dot>
                            Demo dataset
                        </Badge>
                    </header>

                    <Panel>
                        <PanelHeading
                            title="Scan configuration"
                            description="Choose the workspace that will own this scan."
                        />

                        {workspaceState === "loading" && (
                            <p role="status">Loading workspaces…</p>
                        )}

                        {workspaceState === "error" && (
                            <div role="alert" className="scan-page__message scan-page__message--error">
                                <strong>Workspaces unavailable</strong>
                                <p>{workspaceError}</p>
                                <p>
                                    Check your sign-in session and API connection
                                    before launching a scan.
                                </p>
                            </div>
                        )}

                        {workspaceState === "ready" && workspaces.length === 0 && (
                            <div className="scan-page__message">
                                <strong>No workspaces available</strong>
                                <p>
                                    Create or join a workspace before launching a scan.
                                </p>
                            </div>
                        )}

                        {workspaceState === "ready" && workspaces.length > 0 && (
                            <div className="scan-page__form">
                                <label className="scan-page__field">
                                    <span>Workspace</span>
                                    <select
                                        value={workspaceId}
                                        onChange={(event) =>
                                            setWorkspaceId(event.target.value)
                                        }
                                        disabled={submitting}
                                    >
                                        {workspaces.map((workspace) => (
                                            <option
                                                key={workspace.id}
                                                value={workspace.id}
                                            >
                                                {workspace.name}
                                            </option>
                                        ))}
                                    </select>
                                </label>

                                <div className="scan-page__dataset">
                                    <span className="scan-page__dataset-icon">
                                        <Icon name="shield" size={22} />
                                    </span>
                                    <div>
                                        <strong>CloudTrap demo ruleset</strong>
                                        <p>
                                            Dataset ID: <code>default-demo</code>
                                        </p>
                                        <p>
                                            The backend determines the actual scan
                                            status, asset counts, and findings.
                                        </p>
                                    </div>
                                </div>

                                {scanError && (
                                    <div
                                        role="alert"
                                        className="scan-page__message scan-page__message--error"
                                    >
                                        {scanError}
                                    </div>
                                )}

                                <div className="scan-page__actions">
                                    <Button
                                        onClick={handleLaunch}
                                        disabled={!workspaceId || submitting}
                                        loading={submitting}
                                        icon="activity"
                                    >
                                        {submitting ? "Submitting scan…" : "Launch scan"}
                                    </Button>
                                </div>
                            </div>
                        )}
                    </Panel>

                    <Panel>
                        <PanelHeading
                            title="Before you launch"
                            description="What this workflow currently supports."
                        />
                        <ul className="scan-page__notes">
                            <li>
                                Only the backend-supported <code>default-demo</code>{" "}
                                dataset is selectable.
                            </li>
                            <li>
                                The API initially accepts the scan as queued; acceptance
                                does not mean the scan has completed.
                            </li>
                            <li>
                                Scan results are displayed only when returned by the API.
                            </li>
                        </ul>
                    </Panel>
                </div>
            </main>
        </AppShell>
    );
}
