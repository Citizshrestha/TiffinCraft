import { apiGet, apiPut, apiDelete } from "./client";
import { formatJoinedDate } from "../utils/format";
import { StatusType } from "../components/StatusBadge";

export interface BackendOrder {
  id: number;
  customer_id: number;
  cook_id: number;
  total_amount: number | string;
  status: string;
  delivery_address: string;
  payment_method: string;
  payment_status: string;
  created_at: string;
  customer_name: string;
  cook_name: string;
  kitchen_name: string | null;
  items_summary: string | null;
}

export interface Order {
  id: number;
  displayId: string;
  customer: string;
  cook: string;
  product: string;
  amount: number;
  status: StatusType;
  paymentMethod: string;
  paymentStatus: string;
  date: string;
}

export function mapBackendOrder(o: BackendOrder): Order {
  return {
    id: o.id,
    displayId: `#ORD-${o.id}`,
    customer: o.customer_name,
    cook: o.kitchen_name || o.cook_name,
    product: o.items_summary || "—",
    amount: Number(o.total_amount) || 0,
    status: (o.status as StatusType) || "pending",
    paymentMethod: o.payment_method,
    paymentStatus: o.payment_status,
    date: formatJoinedDate(o.created_at),
  };
}

export async function fetchOrders(): Promise<Order[]> {
  const data = await apiGet<{ success: boolean; orders: BackendOrder[] }>("/admin/orders");
  return data.orders.map(mapBackendOrder);
}

export async function updateOrderStatusApi(id: number, status: StatusType): Promise<Order> {
  const data = await apiPut<{ success: boolean; message: string; order: BackendOrder }>(
    `/admin/orders/${id}/status`,
    { status }
  );
  return mapBackendOrder(data.order);
}

export async function deleteOrderApi(id: number): Promise<void> {
  await apiDelete<{ success: boolean; message: string }>(`/admin/orders/${id}`);
}
