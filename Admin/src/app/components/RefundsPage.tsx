import React, { useEffect, useState } from "react";
import { Eye, CheckCircle2 } from "lucide-react";
import { StatusBadge, StatusType } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { Modal, DetailRow, FormField, SaveCancel } from "./Modal";
import { fetchRefunds, processRefundApi, RefundRequest } from "../api/refundsApi";

const PER_PAGE = 10;

const STAT_TABS: { id: string; label: string }[] = [
  { id: "all", label: "All" },
  { id: "requested", label: "Requested" },
  { id: "under_review", label: "Under Review" },
  { id: "approved", label: "Approved" },
  { id: "processed", label: "Processed" },
  { id: "rejected", label: "Rejected" },
];

const PROCESS_OPTIONS: { value: "under_review" | "approved" | "rejected" | "processed"; label: string }[] = [
  { value: "under_review", label: "Mark Under Review" },
  { value: "approved", label: "Approve" },
  { value: "rejected", label: "Reject" },
  { value: "processed", label: "Mark Processed (refund issued via eSewa dashboard)" },
];

export function RefundsPage() {
  const [rows, setRows] = useState<RefundRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [tab, setTab] = useState("all");
  const [page, setPage] = useState(1);
  const [viewing, setViewing] = useState<RefundRequest | null>(null);
  const [processing, setProcessing] = useState<RefundRequest | null>(null);
  const [newStatus, setNewStatus] = useState<"under_review" | "approved" | "rejected" | "processed">("under_review");
  const [adminNotes, setAdminNotes] = useState("");
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await fetchRefunds();
      setRows(data);
    } catch (e: any) {
      setError(e?.message || "Failed to load refund requests.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);
  useEffect(() => { setPage(1); }, [tab, search]);

  const filtered = rows.filter(r => {
    const s = !search
      || r.displayId.toLowerCase().includes(search.toLowerCase())
      || r.customer.toLowerCase().includes(search.toLowerCase())
      || r.orderDisplayId.toLowerCase().includes(search.toLowerCase());
    const t = tab === "all" || r.status === tab;
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible = filtered.slice((page - 1) * PER_PAGE, page * PER_PAGE);

  const startProcess = (r: RefundRequest) => {
    setProcessing(r);
    setNewStatus(r.status === "requested" ? "under_review" : "approved");
    setAdminNotes(r.adminNotes || "");
  };

  const submitProcess = async () => {
    if (!processing) return;
    setSaving(true);
    try {
      await processRefundApi(processing.id, newStatus, adminNotes || undefined);
      setProcessing(null);
      await load();
    } catch (e: any) {
      setError(e?.message || "Failed to update refund request.");
    } finally {
      setSaving(false);
    }
  };

  const totalPending = rows.filter(r => r.status === "requested" || r.status === "under_review")
    .reduce((sum, r) => sum + r.amount, 0);
  const totalProcessed = rows.filter(r => r.status === "processed").reduce((sum, r) => sum + r.amount, 0);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 28, color: "#1c1f29" }}>Refunds</p>
        <p style={{ fontFamily: "Inter", fontWeight: 400, fontSize: 14, color: "#9499a6", marginTop: 4 }}>
          Review refund requests flagged by cooks or customers. Issue the refund via the{" "}
          <a href="https://merchant.esewa.com.np" target="_blank" rel="noreferrer" style={{ color: "#7887fa" }}>
            eSewa merchant dashboard
          </a>{" "}
          first, then mark it processed here.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white flex items-center gap-4 p-5 rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
          <span style={{ fontSize: 28 }}>⏳</span>
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>₹{totalPending.toFixed(0)}</p>
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>Awaiting Review</p>
          </div>
        </div>
        <div className="bg-white flex items-center gap-4 p-5 rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
          <span style={{ fontSize: 28 }}>↩️</span>
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>₹{totalProcessed.toFixed(0)}</p>
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>Refunded</p>
          </div>
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        {STAT_TABS.map(t => (
          <button key={t.id} onClick={() => setTab(t.id)}
            style={{ padding: "10px 16px", borderRadius: 8, fontSize: 13, cursor: "pointer", border: "none", fontFamily: "Inter", fontWeight: 500,
              background: tab === t.id ? "#3b82f6" : "#f2f5f7", color: tab === t.id ? "#fff" : "#9499a6" }}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{ border: "1px solid #e5e8ed" }}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{ border: "none", fontFamily: "Inter", fontSize: 14, color: "#1c1f29" }}
          placeholder="Search by refund ID, order ID, or customer..." value={search} onChange={e => setSearch(e.target.value)} />
      </div>

      {error && (
        <div className="px-4 py-3 rounded-[8px]" style={{ background: "rgba(242,89,89,0.08)", color: "#f25959", fontFamily: "Inter", fontSize: 13 }}>
          {error}
        </div>
      )}

      <div className="bg-white rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1100px]">
            <div className="flex gap-4 pb-4" style={{ borderBottom: "1px solid #e5e8ed" }}>
              <p style={{ width: 40, flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 12, color: "#9499a6" }}>S.N</p>
              {["Refund ID", "Order ID", "Customer", "Requested By", "Reason", "Amount", "Status", "Date", "Actions"].map((h, i) => (
                <p key={h} style={{ width: [100, 100, 130, 120, 140, 90, 110, 100, 90][i], flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 12, color: "#9499a6" }}>{h}</p>
              ))}
            </div>
            {loading && (
              <p style={{ fontFamily: "Inter", fontSize: 14, color: "#9499a6", textAlign: "center", padding: "32px 0" }}>Loading refund requests…</p>
            )}
            {!loading && visible.length === 0 && (
              <p style={{ fontFamily: "Inter", fontSize: 14, color: "#9499a6", textAlign: "center", padding: "32px 0" }}>No refund requests found.</p>
            )}
            {!loading && visible.map((r, idx) => (
              <div key={r.id}>
                <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                  <p style={{ width: 40, flexShrink: 0, fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#9499a6" }}>{(page - 1) * PER_PAGE + idx + 1}</p>
                  <p style={{ width: 100, flexShrink: 0, fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#7887fa" }}>{r.displayId}</p>
                  <p style={{ width: 100, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#7887fa" }}>{r.orderDisplayId}</p>
                  <p style={{ width: 130, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>{r.customer}</p>
                  <p style={{ width: 120, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>{r.requestedBy} <span style={{ color: "#9499a6" }}>({r.requestedByRole})</span></p>
                  <p style={{ width: 140, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>{r.reason}</p>
                  <p style={{ width: 90, flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 13, color: "#1c1f29" }}>₹{r.amount.toFixed(0)}</p>
                  <div style={{ width: 110, flexShrink: 0 }}><StatusBadge status={r.status} /></div>
                  <p style={{ width: 100, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>{r.date}</p>
                  <div style={{ width: 90, flexShrink: 0, display: "flex", gap: 4 }}>
                    <button title="View" onClick={() => setViewing(r)}
                      style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: "rgba(120,135,250,0.10)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
                      <Eye size={14} color="#7887fa" />
                    </button>
                    {r.status !== "processed" && r.status !== "rejected" && (
                      <button title="Process" onClick={() => startProcess(r)}
                        style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: "rgba(16,185,129,0.10)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
                        <CheckCircle2 size={14} color="#10b981" />
                      </button>
                    )}
                  </div>
                </div>
                {idx < visible.length - 1 && <div style={{ height: 1, background: "#f2f5f7" }} />}
              </div>
            ))}
          </div>
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage} />

      {viewing && (
        <Modal title="Refund Request Details" onClose={() => setViewing(null)}>
          <DetailRow label="Refund ID" value={<span style={{ color: "#7887fa", fontWeight: 600 }}>{viewing.displayId}</span>} />
          <DetailRow label="Order ID" value={<span style={{ color: "#7887fa" }}>{viewing.orderDisplayId}</span>} />
          <DetailRow label="Customer" value={viewing.customer} />
          <DetailRow label="Cook / Kitchen" value={viewing.cook} />
          <DetailRow label="Requested By" value={`${viewing.requestedBy} (${viewing.requestedByRole})`} />
          <DetailRow label="Reason" value={viewing.reason} />
          <DetailRow label="Notes" value={viewing.reasonNotes || "—"} />
          <DetailRow label="Refund Amount" value={<span style={{ fontWeight: 700 }}>₹{viewing.amount.toFixed(0)}</span>} />
          <DetailRow label="Status" value={<StatusBadge status={viewing.status} />} />
          <DetailRow label="Admin Notes" value={viewing.adminNotes || "—"} />
          <DetailRow label="Processed By" value={viewing.processedBy || "—"} />
          <DetailRow label="Requested On" value={viewing.date} />
        </Modal>
      )}

      {processing && (
        <Modal title={`Process ${processing.displayId}`} onClose={() => setProcessing(null)}>
          <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", marginBottom: 16 }}>
            Refund amount: <strong style={{ color: "#1c1f29" }}>₹{processing.amount.toFixed(0)}</strong> for order {processing.orderDisplayId}.
            {newStatus === "processed" && (
              <> Make sure you've already issued this refund via the eSewa merchant dashboard before marking it processed.</>
            )}
          </p>
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>New Status</p>
            <select
              value={newStatus}
              onChange={e => setNewStatus(e.target.value as typeof newStatus)}
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", background: "white", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 14, outline: "none" }}
            >
              {PROCESS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <FormField label="Admin Notes (optional)" value={adminNotes} onChange={setAdminNotes} />
          <SaveCancel onCancel={() => setProcessing(null)} onSave={submitProcess} saving={saving} saveLabel="Update" />
        </Modal>
      )}
    </div>
  );
}
