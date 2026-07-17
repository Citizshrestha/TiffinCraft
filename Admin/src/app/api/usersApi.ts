import { apiGet, apiPost, apiPut, apiDelete } from "./client";
import { capitalizeRole, formatJoinedDate } from "../utils/format";

export type UStatus = "active" | "inactive";

export interface BackendUser {
  id: number;
  full_name: string;
  email: string;
  phone: string;
  role: string;
  is_active: 0 | 1 | boolean;
  is_verified?: 0 | 1 | boolean;
  created_at: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  phone: string;
  status: UStatus;
  joined: string;
}

export interface CreateUserPayload {
  full_name: string;
  email: string;
  phone: string;
  role: string;
  password: string;
  is_active?: boolean;
}

export interface UpdateUserPayload {
  full_name: string;
  email: string;
  phone: string;
  role: string;
  is_active: boolean;
}

export function mapBackendUser(u: BackendUser): User {
  return {
    id: u.id,
    name: u.full_name,
    email: u.email,
    role: capitalizeRole(u.role),
    phone: u.phone,
    status: u.is_active === 1 || u.is_active === true ? "active" : "inactive",
    joined: formatJoinedDate(u.created_at),
  };
}

export async function fetchUsers(): Promise<User[]> {
  const data = await apiGet<{ success: boolean; users: BackendUser[] }>("/admin/users");
  return data.users.map(mapBackendUser);
}

export async function createUserApi(payload: CreateUserPayload): Promise<User> {
  const data = await apiPost<{ success: boolean; message: string; user: BackendUser }>(
    "/admin/users",
    payload
  );
  return mapBackendUser(data.user);
}

export async function updateUserApi(id: number, payload: UpdateUserPayload): Promise<User> {
  const data = await apiPut<{ success: boolean; message: string; user: BackendUser }>(
    `/admin/users/${id}`,
    payload
  );
  return mapBackendUser(data.user);
}

export async function deleteUserApi(id: number): Promise<void> {
  await apiDelete<{ success: boolean; message: string }>(`/admin/users/${id}`);
}
