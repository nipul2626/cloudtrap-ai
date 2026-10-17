
import type {
    ApiErrorResponse,
    CloudAsset,
    CsrfResponse,
    Finding,
    LoginResponse,
    PageResponse, RegisterResponse,
    Scan,
    ScanFindingsResponse,
    WorkspacePageResponse,

} from "./api-types";

const API_BASE_URL = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
).replace(/\/+$/, "");

export class ApiError extends Error {
    constructor(
        message: string,
        public readonly status: number,
        public readonly code?: string,
        public readonly requestId?: string,
    ) {
        super(message);
        this.name = "ApiError";
    }
}

let csrfCache: CsrfResponse | null = null;
let csrfRequest: Promise<CsrfResponse> | null = null;

function readCookie(name: string): string | null {
    if (typeof document === "undefined") return null;

    const prefix = `${name}=`;
    const entry = document.cookie
        .split("; ")
        .find((cookie) => cookie.startsWith(prefix));

    return entry ? decodeURIComponent(entry.slice(prefix.length)) : null;
}

async function parseError(response: Response): Promise<ApiError> {
    let body: ApiErrorResponse | null = null;

    try {
        body = (await response.json()) as ApiErrorResponse;
    } catch {
        // Some infrastructure errors return a non-JSON body.
    }

    return new ApiError(
        body?.message ?? body?.error ?? `Request failed (${response.status})`,
        response.status,
        body?.code,
        body?.requestId ?? response.headers.get("X-Request-Id") ?? undefined,
    );
}

async function requestCsrf(): Promise<CsrfResponse> {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/csrf`, {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        headers: { Accept: "application/json" },
    });

    if (!response.ok) {
        throw await parseError(response);
    }

    const result = (await response.json()) as CsrfResponse;

    if (!result.token || !result.headerName) {
        throw new Error("The API returned an invalid CSRF response.");
    }

    csrfCache = result;
    return result;
}

export async function getCsrfToken(): Promise<CsrfResponse> {
    if (csrfCache) return csrfCache;

    if (!csrfRequest) {
        csrfRequest = requestCsrf().finally(() => {
            csrfRequest = null;
        });
    }

    return csrfRequest;
}

export function clearCsrfCache(): void {
    csrfCache = null;
}

type RequestOptions = Omit<RequestInit, "body"> & {
    body?: unknown;
    csrf?: boolean;
};

async function apiRequest<T>(
    path: string,
    options: RequestOptions = {},
): Promise<T> {
    const method = (options.method ?? "GET").toUpperCase();
    const hasBody = options.body !== undefined;
    const changesState = !["GET", "HEAD", "OPTIONS"].includes(method);
    const headers = new Headers(options.headers);

    headers.set("Accept", "application/json");

    if (hasBody) {
        headers.set("Content-Type", "application/json");
    }

    if (changesState && options.csrf !== false) {
        const csrf = await getCsrfToken();
        headers.set(csrf.headerName, csrf.token);
    }

    // Preserve the CSRF cookie value when available. Spring Security validates
    // the request header against the token associated with the session/cookie.
    const cookieToken = readCookie("XSRF-TOKEN");

    if (cookieToken && changesState && options.csrf !== false) {
        const csrf = await getCsrfToken();
        headers.set(csrf.headerName, csrf.token);
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        method,
        headers,
        credentials: "include",
        cache: "no-store",
        body: hasBody ? JSON.stringify(options.body) : undefined,
    });

    if (!response.ok) {
        if (response.status === 403) {
            clearCsrfCache();
        }

        throw await parseError(response);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

export const api = {
    get: <T>(path: string) => apiRequest<T>(path),

    post: <T>(path: string, body?: unknown, headers?: HeadersInit) =>
        apiRequest<T>(path, {
            method: "POST",
            body,
            headers,
        }),

    patch: <T>(path: string, body: unknown) =>
        apiRequest<T>(path, {
            method: "PATCH",
            body,
        }),

    delete: <T>(path: string) =>
        apiRequest<T>(path, {
            method: "DELETE",
        }),
};

export async function register(
    email: string,
    password: string,
    displayName: string,
): Promise<RegisterResponse> {
    return api.post<RegisterResponse>("/api/v1/auth/register", {
        email,
        password,
        displayName,
    });
}
export async function login(
    email: string,
    password: string,
): Promise<LoginResponse> {
    const result = await api.post<LoginResponse>("/api/v1/auth/login", {
        email,
        password,
    });

    // The session may have changed during login; refresh before later writes.
    clearCsrfCache();
    return result;
}

export async function logout(): Promise<void> {
    await api.post<void>("/api/v1/auth/logout");
    clearCsrfCache();
}

export async function getWorkspaces(): Promise<WorkspacePageResponse> {
    return api.get<WorkspacePageResponse>(
        "/api/v1/workspaces?page=0&size=100",
    );
}

export async function getAssets(
    workspaceId: string,
    page = 0,
    size = 50,
): Promise<PageResponse<CloudAsset>> {
    const query = new URLSearchParams({
        workspaceId,
        page: String(page),
        size: String(size),
    });

    return api.get<PageResponse<CloudAsset>>(`/api/v1/assets?${query}`);
}

export async function getScan(
    scanId: string,
    workspaceId: string,
): Promise<Scan> {
    const query = new URLSearchParams({ workspaceId });

    return api.get<Scan>(`/api/v1/scans/${scanId}?${query}`);
}

export async function getScanFindings(
    scanId: string,
    workspaceId: string,
    page = 0,
    size = 50,
): Promise<ScanFindingsResponse> {
    const query = new URLSearchParams({
        workspaceId,
        page: String(page),
        size: String(size),
    });

    return api.get<ScanFindingsResponse>(
        `/api/v1/scans/${scanId}/findings?${query}`,
    );
}

export async function createDemoScan(
    workspaceId: string,
    idempotencyKey?: string,
): Promise<Scan> {
    const headers = idempotencyKey
        ? { "Idempotency-Key": idempotencyKey }
        : undefined;

    return api.post<Scan>(
        "/api/v1/scans",
        { workspaceId, datasetId: "default-demo" },
        headers,
    );
}

export async function updateFindingStatus(
    findingId: string,
    workspaceId: string,
    status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED" | "ACCEPTED_RISK",
): Promise<Finding> {
    const query = new URLSearchParams({ workspaceId });

    return api.patch<Finding>(
        `/api/v1/findings/${findingId}/status?${query}`,
        { status },
    );
}
