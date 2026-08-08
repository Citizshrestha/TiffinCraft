import React from "react";

type StatusType =
  | "delivered"
  | "completed"
  | "active"
  | "paid"
  | "accepted"
  | "processing"
  | "pending"
  | "out_for_delivery"
  | "cancelled"
  | "inactive"
  | "failed"
  | "refunded"
  | "verified"
  | "unverified"
  | "confirmed"
  | "preparing"
  | "ready"
  | "requested"
  | "under_review"
  | "approved"
  | "rejected"
  | "processed"
  | "submitted"
  | "overdue";

const statusConfig: Record<
  StatusType,
  { bg: string; color: string; label: string }
> = {
  delivered: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Delivered",
  },
  completed: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Completed",
  },
  active: { bg: "rgba(87,184,105,0.15)", color: "#57b869", label: "Active" },
  paid: { bg: "rgba(87,184,105,0.12)", color: "#57b869", label: "Paid" },
  accepted: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Accepted",
  },
  verified: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Verified",
  },
  processing: {
    bg: "rgba(242,199,64,0.12)",
    color: "#f2c740",
    label: "Processing",
  },
  pending: {
    bg: "rgba(242,140,64,0.12)",
    color: "#f28c40",
    label: "Pending",
  },
  out_for_delivery: {
    bg: "rgba(120,135,250,0.12)",
    color: "#7887fa",
    label: "Out for Delivery",
  },
  cancelled: {
    bg: "rgba(242,89,89,0.12)",
    color: "#f25959",
    label: "Cancelled",
  },
  inactive: {
    bg: "rgba(242,89,89,0.15)",
    color: "#f25959",
    label: "Inactive",
  },
  failed: { bg: "rgba(242,89,89,0.12)", color: "#f25959", label: "Failed" },
  refunded: {
    bg: "rgba(120,135,250,0.12)",
    color: "#7887fa",
    label: "Refunded",
  },
  unverified: {
    bg: "rgba(242,199,64,0.12)",
    color: "#f2c740",
    label: "Unverified",
  },
  confirmed: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Confirmed",
  },
  preparing: {
    bg: "rgba(242,199,64,0.12)",
    color: "#f2c740",
    label: "Preparing",
  },
  ready: {
    bg: "rgba(120,135,250,0.12)",
    color: "#7887fa",
    label: "Ready",
  },
  requested: {
    bg: "rgba(242,140,64,0.12)",
    color: "#f28c40",
    label: "Requested",
  },
  under_review: {
    bg: "rgba(242,199,64,0.12)",
    color: "#f2c740",
    label: "Under Review",
  },
  approved: {
    bg: "rgba(87,184,105,0.12)",
    color: "#57b869",
    label: "Approved",
  },
  rejected: {
    bg: "rgba(242,89,89,0.12)",
    color: "#f25959",
    label: "Rejected",
  },
  processed: {
    bg: "rgba(120,135,250,0.12)",
    color: "#7887fa",
    label: "Processed",
  },
  submitted: {
    bg: "rgba(120,135,250,0.12)",
    color: "#7887fa",
    label: "Submitted",
  },
  overdue: {
    bg: "rgba(242,89,89,0.12)",
    color: "#f25959",
    label: "Overdue",
  },
};

interface StatusBadgeProps {
  status: StatusType;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status];
  return (
    <span
      className="inline-flex items-center px-[10px] py-[4px] rounded-[12px] text-[12px] whitespace-nowrap"
      style={{
        background: config.bg,
        color: config.color,
        fontFamily: "Inter",
        fontWeight: 600,
      }}
    >
      {config.label}
    </span>
  );
}

export type { StatusType };
