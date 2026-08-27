import React, { useState, useEffect, useCallback, useRef } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, DetailRow } from "./Modal";
import { exportCSV } from "../utils/csv";
import { fetchAdminPayments, AdminPayment, PaymentStats } from "../api/paymentsApi";

const PER_PAGE = 10;

const STAT_TABS = [
  { id: "all", label: "All" },
  { id: "paid", label: "Paid" },
  { id: "pending", label: "Pending" },
  { id: "refunded", label: "Refunded" },
  { id: "failed", label: "Failed" },
];

function SkeletonRow() {
  return (
    <div className="flex gap-4 items-center py-4">
      {[40, 100, 100, 130, 130, 75, 100, 100, 100, 110].map((w, i) => (
        <div
          key={i}
          style={{
            width: w,
            flexShrink: 0,
            height: 12,
            borderRadius: 6,
            background: "#f0f2f5",
          }}
        />
      ))}
    </div>
  );
}

export function PaymentsPage() {
  const [stats, setStats] = useState<PaymentStats | null>(null);
  const [rows, setRows] = useState<AdminPayment[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [search, setSearch] = useState("");
  const [tab, setTab] = useState("all");
  const [page, setPage] = useState(1);
  const [viewing, setViewing] = useState<AdminPayment | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  // Debounce search
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [debouncedSearch, setDebouncedSearch] = useState("");

  const handleSearchChange = (val: string) => {
    setSearch(val);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      setDebouncedSearch(val);
      setPage(1);
    }, 400);
  };

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const res = await fetchAdminPayments({
        page,
        limit: PER_PAGE,
        search: debouncedSearch || undefined,
        status: tab !== "all" ? tab : undefined,
      });
      setStats(res.stats);
      setRows(res.payments);
      setTotalPages(res.pagination.totalPages);
      setTotalItems(res.pagination.total);
    } catch (err) {
      setLoadError(
        err instanceof Error ? err.message : "Failed to load payments."
      );
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, tab]);

  useEffect(() => {
    load();
  }, [load]);

  // Reset page on filter change
  useEffect(() => {
    setPage(1);
  }, [tab]);

  const handleExport = () =>
    exportCSV(
      "payments.csv",
      rows.map((p) => ({
        "Payment ID": p.id,
        "Order ID": p.orderId,
        Customer: p.customer,
        Cook: p.cook,
        Amount: p.amount,
        Method: p.method,
        Status: p.status,
        Date: p.date,
      }))
    );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p
            style={{
              fontFamily: "Inter",
              fontWeight: 700,
              fontSize: 28,
              color: "#1c1f29",
            }}
          >
            Payments
          </p>
          <p
            style={{
              fontFamily: "Inter",
              fontWeight: 400,
              fontSize: 14,
              color: "#9499a6",
              marginTop: 4,
            }}
          >
            View and manage all payment transactions.
          </p>
        </div>
        <button
          onClick={handleExport}
          className="self-start shrink-0"
          style={{
            padding: "10px 16px",
            borderRadius: 8,
            border: "1px solid #e5e8ed",
            background: "white",
            fontFamily: "Inter",
            fontWeight: 500,
            fontSize: 13,
            color: "#9499a6",
            cursor: "pointer",
          }}
        >
          Export CSV
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[
          {
            label: "Total Collected",
            value: stats ? `₹${stats.total_collected.toLocaleString()}` : "—",
            icon: "💰",
          },
          {
            label: "Pending Payouts",
            value: stats ? `₹${stats.pending_payouts.toLocaleString()}` : "—",
            icon: "⏳",
          },
          {
            label: "Refunds Issued",
            value: stats ? `₹${stats.refunds_issued.toLocaleString()}` : "—",
            icon: "↩️",
          },
          {
            label: "Failed Transactions",
            value: stats ? String(stats.failed_transactions) : "—",
            icon: "❌",
          },
        ].map((s) => (
          <div
            key={s.label}
            className="bg-white flex items-center gap-4 p-5 rounded-[12px] flex-1"
            style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}
          >
            <span style={{ fontSize: 28 }}>{s.icon}</span>
            <div>
              <p
                style={{
                  fontFamily: "Inter",
                  fontWeight: 700,
                  fontSize: 20,
                  color: "#1c1f29",
                }}
              >
                {s.value}
              </p>
              <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>
                {s.label}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Status filter tabs */}
      <div className="flex gap-2 flex-wrap">
        {STAT_TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            style={{
              padding: "10px 16px",
              borderRadius: 8,
              fontSize: 13,
              cursor: "pointer",
              border: "none",
              fontFamily: "Inter",
              fontWeight: 500,
              background: tab === t.id ? "#3b82f6" : "#f2f5f7",
              color: tab === t.id ? "#fff" : "#9499a6",
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div
        className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white"
        style={{ border: "1px solid #e5e8ed" }}
      >
        <span>🔍</span>
        <input
          className="flex-1 outline-none bg-transparent"
          style={{
            border: "none",
            fontFamily: "Inter",
            fontSize: 14,
            color: "#1c1f29",
          }}
          placeholder="Search payments..."
          value={search}
          onChange={(e) => handleSearchChange(e.target.value)}
        />
      </div>

      {/* Error banner */}
      {loadError && (
        <div
          className="flex items-center gap-3 px-4 py-3 rounded-[10px]"
          style={{ background: "#fef2f2", border: "1px solid #fecaca" }}
        >
          <span style={{ fontSize: 16 }}>⚠️</span>
          <p
            style={{
              fontFamily: "Inter",
              fontSize: 14,
              color: "#dc2626",
              flex: 1,
            }}
          >
            {loadError}
          </p>
          <button
            onClick={load}
            style={{
              background: "#dc2626",
              color: "white",
              border: "none",
              borderRadius: 6,
              padding: "6px 14px",
              fontFamily: "Inter",
              fontWeight: 600,
              fontSize: 13,
              cursor: "pointer",
            }}
          >
            Retry
          </button>
        </div>
      )}

      <div
        className="bg-white rounded-[12px]"
        style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}
      >
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1140px]">
            <div
              className="flex gap-4 pb-4"
              style={{ borderBottom: "1px solid #e5e8ed" }}
            >
              <p
                style={{
                  width: 40,
                  flexShrink: 0,
                  fontFamily: "Inter",
                  fontWeight: 600,
                  fontSize: 12,
                  color: "#9499a6",
                }}
              >
                S.N
              </p>
              {[
                "Pay ID",
                "Order ID",
                "Customer",
                "Cook",
                "Amount",
                "Method",
                "Status",
                "Date",
                "Actions",
              ].map((h, i) => (
                <p
                  key={h}
                  style={{
                    width: [100, 100, 130, 130, 75, 100, 100, 100, 110][i],
                    flexShrink: 0,
                    fontFamily: "Inter",
                    fontWeight: 600,
                    fontSize: 12,
                    color: "#9499a6",
                  }}
                >
                  {h}
                </p>
              ))}
            </div>

            {/* Loading skeletons */}
            {loading &&
              Array.from({ length: 5 }).map((_, i) => (
                <div key={i}>
                  <SkeletonRow />
                  {i < 4 && (
                    <div style={{ height: 1, background: "#f5f7fa" }} />
                  )}
                </div>
              ))}

            {/* Empty state */}
            {!loading && rows.length === 0 && (
              <p
                style={{
                  fontFamily: "Inter",
                  fontSize: 14,
                  color: "#9499a6",
                  textAlign: "center",
                  padding: "32px 0",
                }}
              >
                {debouncedSearch || tab !== "all"
                  ? "No payments match your filters."
                  : "No payments found."}
              </p>
            )}

            {/* Data rows */}
            {!loading &&
              rows.map((p, idx) => (
                <div key={p.id}>
                  <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                    <p
                      style={{
                        width: 40,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontWeight: 500,
                        fontSize: 13,
                        color: "#9499a6",
                      }}
                    >
                      {(page - 1) * PER_PAGE + idx + 1}
                    </p>
                    <p
                      style={{
                        width: 100,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontWeight: 500,
                        fontSize: 13,
                        color: "#7887fa",
                      }}
                    >
                      {p.id}
                    </p>
                    <p
                      style={{
                        width: 100,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#7887fa",
                      }}
                    >
                      {p.orderId}
                    </p>
                    <p
                      style={{
                        width: 130,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#1c1f29",
                      }}
                    >
                      {p.customer}
                    </p>
                    <p
                      style={{
                        width: 130,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#1c1f29",
                      }}
                    >
                      {p.cook}
                    </p>
                    <p
                      style={{
                        width: 75,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontWeight: 600,
                        fontSize: 13,
                        color: "#1c1f29",
                      }}
                    >
                      {p.amount}
                    </p>
                    <p
                      style={{
                        width: 100,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#1c1f29",
                      }}
                    >
                      {p.method}
                    </p>
                    <div style={{ width: 100, flexShrink: 0 }}>
                      <StatusBadge status={p.status as StatusType} />
                    </div>
                    <p
                      style={{
                        width: 100,
                        flexShrink: 0,
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#9499a6",
                      }}
                    >
                      {p.date}
                    </p>
                    <div style={{ width: 110, flexShrink: 0 }}>
                      <ActionButtons onView={() => setViewing(p)} />
                    </div>
                  </div>
                  {idx < rows.length - 1 && (
                    <div style={{ height: 1, background: "#f2f5f7" }} />
                  )}
                </div>
              ))}
          </div>
        </div>
      </div>

      <Pagination
        current={page}
        total={totalPages}
        totalItems={totalItems}
        onPageChange={setPage}
      />

      {viewing && (
        <Modal title="Payment Details" onClose={() => setViewing(null)}>
          <DetailRow
            label="Payment ID"
            value={
              <span style={{ color: "#7887fa", fontWeight: 600 }}>
                {viewing.id}
              </span>
            }
          />
          <DetailRow
            label="Order ID"
            value={<span style={{ color: "#7887fa" }}>{viewing.orderId}</span>}
          />
          <DetailRow label="Customer" value={viewing.customer} />
          <DetailRow label="Cook" value={viewing.cook} />
          <DetailRow
            label="Amount"
            value={<span style={{ fontWeight: 700 }}>{viewing.amount}</span>}
          />
          <DetailRow label="Method" value={viewing.method} />
          <DetailRow
            label="Status"
            value={<StatusBadge status={viewing.status as StatusType} />}
          />
          <DetailRow label="Date" value={viewing.date} />
          {viewing.refundStatus && (
            <DetailRow
              label="Refund Status"
              value={
                <span style={{ color: "#f59e0b", fontWeight: 600 }}>
                  {viewing.refundStatus}
                </span>
              }
            />
          )}
        </Modal>
      )}
    </div>
  );
}
