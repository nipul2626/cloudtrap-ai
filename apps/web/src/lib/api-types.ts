
export type ApiErrorResponse = {
    timestamp?: string;
    status?: number;
    error?: string;
    code?: string;
    message?: string;
    path?: string;
    requestId?: string;
    details?: unknown[];
};

export type PageResponse<T> = {
    items: T[];
    page: number;
    size: number;
    total: number;
};

export type Workspace = {
    id: string;
    name: string;
    slug: string;
    createdBy: string;
    createdAt: string;
    updatedAt: string;
};

export type WorkspacePageResponse = {
    content: Workspace[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
};

export type CloudAsset = {
    id: string;
    provider: string;
    external_id: string;
    asset_type: string;
    name: string;
    region: string | null;
    exposure: string | null;
    criticality: number | null;
    first_seen_at?: string;
    last_seen_at?: string;
};

export type Finding = {
    id: string;
    asset_id?: string;
    rule_id?: string;
    title: string;
    description?: string;
    category?: string;
    severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | string;
    status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED" | "ACCEPTED_RISK" | string;
    risk_score?: number | null;
    first_detected_at?: string;
    last_detected_at?: string;
};

export type Scan = {
    id: string;
    workspace_id: string;
    dataset_id?: string;
    source: string;
    status: "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED" | string;
    queued_at?: string;
    started_at?: string | null;
    completed_at?: string | null;
    assets_discovered?: number;
    assets_scanned?: number;
    findings_created?: number;
    error_code?: string | null;
    safe_error_message?: string | null;
};

export type CsrfResponse = {
    token: string;
    headerName: string;
};

export type LoginResponse = {
    authenticated: boolean;
    email: string;
};

export type RegisterResponse = {
    id: string;
    email: string;
    displayName: string;
    createdAt: string;
};

export type ScanFindingsResponse = {
    scan: Scan;
    items: Finding[];
    page: number;
    size: number;
    total: number;
};
