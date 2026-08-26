import { apiGet, apiPost, apiPut, apiDelete, BACKEND_ORIGIN } from "./client";
import { formatJoinedDate } from "../utils/format";

export function resolveImageUrl(url?: string | null): string {
  if (!url) return "";
  if (/^https?:\/\//i.test(url)) return url;
  return `${BACKEND_ORIGIN}${url.startsWith("/") ? "" : "/"}${url}`;
}

export type CookVerification = "verified" | "unverified";

export interface BackendCook {
  user_id: number;
  kitchen_name: string | null;
  rating: number | string;
  total_orders: number;
  is_verified: 0 | 1 | boolean;
  is_approved: 0 | 1 | boolean;
  full_name: string;
  email: string;
  phone: string;
  is_active: 0 | 1 | boolean;
  profile_image?: string | null;
  created_at: string;
}

export interface Cook {
  id: number;
  kitchen: string;
  owner: string;
  email: string;
  phone: string;
  rating: number;
  orders: number;
  status: CookVerification;
  joined: string;
  image: string;
}

export interface CreateCookPayload {
  full_name: string;
  email: string;
  phone: string;
  password: string;
  kitchen_name: string;
  rating?: number;
  is_verified?: boolean;
}

export interface UpdateCookPayload {
  full_name: string;
  kitchen_name: string;
  rating: number;
  is_verified: boolean;
}

export function mapBackendCook(c: BackendCook): Cook {
  return {
    id: c.user_id,
    kitchen: c.kitchen_name || "Unnamed Kitchen",
    owner: c.full_name,
    email: c.email,
    phone: c.phone,
    rating: Number(c.rating) || 0,
    orders: c.total_orders || 0,
    status: c.is_verified === 1 || c.is_verified === true ? "verified" : "unverified",
    joined: formatJoinedDate(c.created_at),
    image: resolveImageUrl(c.profile_image),
  };
}

export async function fetchCooks(): Promise<Cook[]> {
  const data = await apiGet<{ success: boolean; cooks: BackendCook[] }>("/admin/cooks");
  return data.cooks.map(mapBackendCook);
}

export async function createCookApi(payload: CreateCookPayload): Promise<Cook> {
  const data = await apiPost<{ success: boolean; message: string; cook: BackendCook }>(
    "/admin/cooks",
    payload
  );
  return mapBackendCook(data.cook);
}

export async function updateCookApi(id: number, payload: UpdateCookPayload): Promise<Cook> {
  const data = await apiPut<{ success: boolean; message: string; cook: BackendCook }>(
    `/admin/cooks/${id}`,
    payload
  );
  return mapBackendCook(data.cook);
}

export async function deleteCookApi(id: number): Promise<void> {
  await apiDelete<{ success: boolean; message: string }>(`/admin/users/${id}`);
}

// ─── Pending / Approval ────────────────────────────────────────
export interface PendingBackendCook extends BackendCook {
  bio?: string;
  specialties?: string;
  capacity?: number | string;
}

export interface PendingCook {
  id: number;
  kitchen: string;
  owner: string;
  email: string;
  phone: string;
  bio: string;
  specialties: string;
  capacity: number;
  rating: number;
  status: CookVerification;
  joined: string;
  image: string;
}

export function mapPendingCook(c: PendingBackendCook): PendingCook {
  return {
    id: c.user_id,
    kitchen: c.kitchen_name || "Unnamed Kitchen",
    owner: c.full_name,
    email: c.email,
    phone: c.phone,
    bio: c.bio || "",
    specialties: c.specialties || "",
    capacity: Number(c.capacity) || 0,
    rating: Number(c.rating) || 0,
    status: c.is_verified === 1 || c.is_verified === true ? "verified" : "unverified",
    joined: formatJoinedDate(c.created_at),
    image: resolveImageUrl(c.profile_image),
  };
}

export async function fetchPendingCooks(): Promise<PendingCook[]> {
  const data = await apiGet<{ success: boolean; cooks: PendingBackendCook[] }>("/admin/cooks/pending");
  return data.cooks.map(mapPendingCook);
}

export async function approveCookApi(cookId: number): Promise<void> {
  await apiPut<{ success: boolean; message: string }>(`/admin/cooks/${cookId}/approve`);
}

export async function rejectCookApi(cookId: number, reason: string): Promise<void> {
  await apiPut<{ success: boolean; message: string }>(`/admin/cooks/${cookId}/reject`, { reason });
}
