// Lightweight fetch-based API client for the Admin panel.
// Base URL is configurable via VITE_API_BASE_URL, falling back to the local backend.
const API_BASE_URL: string =
  (import.meta as any).env?.VITE_API_BASE_URL || "http://localhost:5000/api";

export const BACKEND_ORIGIN: string = API_BASE_URL.replace(/\/api\/?$/, "");

export const TOKEN_KEY = "tc_admin_token";
export const USER_KEY = "tc_admin_user";

export class ApiError extends Error {
  status: number;
  data: unknown;
  constructor(message: string, status: number, data: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

interface ApiOptions extends RequestInit {
  /** Set to false to skip attaching the Authorization header (e.g. login). */
  auth?: boolean;
}

function extractMessage(data: any, fallback: string): string {
  if (!data) return fallback;
  if (typeof data.message === "string") return data.message;
  if (Array.isArray(data.errors) && data.errors[0]?.msg) return data.errors[0].msg;
  return fallback;
}

async function request<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { auth = true, headers, ...rest } = options;
  const token = localStorage.getItem(TOKEN_KEY);

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: {
        "Content-Type": "application/json",
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
    });
  } catch {
    throw new ApiError(
      "Could not reach the server. Please check your connection and try again.",
      0,
      null
    );
  }

  const isJson = res.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await res.json().catch(() => null) : null;

  if (!res.ok) {
    throw new ApiError(extractMessage(data, "Request failed."), res.status, data);
  }

  return data as T;
}

export const apiGet = <T,>(path: string, options?: ApiOptions) =>
  request<T>(path, { ...options, method: "GET" });

export const apiPost = <T,>(path: string, body?: unknown, options?: ApiOptions) =>
  request<T>(path, {
    ...options,
    method: "POST",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

export const apiPut = <T,>(path: string, body?: unknown, options?: ApiOptions) =>
  request<T>(path, {
    ...options,
    method: "PUT",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

export const apiDelete = <T,>(path: string, options?: ApiOptions) =>
  request<T>(path, { ...options, method: "DELETE" });
