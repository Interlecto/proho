// Thin client for the ProHo backend (api/openapi/openapi.yaml). Talks to
// localhost:8080 in dev; see docs/architecture/client-screens.md for the
// workflow this follows.

const API_BASE = "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
  ) {
    super(code);
  }
}

export interface Account {
  id: string;
  email: string;
  displayName: string;
  locale: string;
  status: string;
}

export interface Community {
  id: string;
  key: string;
  name: string;
  legalName: string | null;
  taxId: string | null;
  kind: string;
  plan: string | null;
  country: string;
  currency: string;
  timezone: string;
  address: string | null;
}

interface LoginResponse {
  token: string;
  expiresAt: string;
  account: Account;
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (!response.ok) {
    let code = "unknown_error";
    try {
      const body = (await response.json()) as { error?: string };
      code = body.error ?? code;
    } catch {
      // No JSON body (e.g. a 401 that never reached a controller).
    }
    throw new ApiError(response.status, code);
  }
  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function registerAccount(
  email: string,
  password: string,
  displayName: string,
): Promise<Account> {
  return request<Account>("/accounts", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName }),
  });
}

export function login(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function me(token: string): Promise<Account> {
  return request<Account>("/me", {}, token);
}

export function logout(token: string): Promise<void> {
  return request<void>("/auth/logout", { method: "POST" }, token);
}

export function listCommunities(token: string): Promise<Community[]> {
  return request<Community[]>("/communities", {}, token);
}

export function createCommunity(
  token: string,
  input: { key: string; name: string; kind: string },
): Promise<Community> {
  return request<Community>(
    "/communities",
    { method: "POST", body: JSON.stringify(input) },
    token,
  );
}
