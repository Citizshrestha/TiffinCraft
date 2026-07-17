import { apiPost, apiPut, TOKEN_KEY, USER_KEY } from "./client";

export interface AdminUser {
  id: number;
  full_name: string;
  email: string;
  phone: string;
  role: string;
  profile_image?: string | null;
}

interface LoginResponse {
  success: boolean;
  message: string;
  token: string;
  user: AdminUser;
}

export async function loginAdmin(email: string, password: string): Promise<AdminUser> {
  const data = await apiPost<LoginResponse>(
    "/auth/login",
    { email, password },
    { auth: false }
  );

  if (!data.user || data.user.role !== "admin") {
    throw new Error("Access denied. This account does not have admin privileges.");
  }

  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USER_KEY, JSON.stringify(data.user));
  return data.user;
}

export function logoutAdmin(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredAdmin(): AdminUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AdminUser;
  } catch {
    return null;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/** Persists an updated admin profile (e.g. after a settings save) to localStorage. */
export function updateStoredAdmin(user: AdminUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export async function changeAdminPassword(currentPassword: string, newPassword: string): Promise<void> {
  await apiPut<{ success: boolean; message: string }>("/auth/change-password", {
    currentPassword,
    newPassword,
  });
}
