import React, { useEffect, useRef, useState } from "react";
import { Eye, CheckCircle2, Wallet } from "lucide-react";
import { StatusBadge } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { Modal, DetailRow, FormField, SaveCancel } from "./Modal";
import { useToast } from "./Toast";
import {
  fetchSettlements,
  verifySettlementApi,
  generateSettlementsApi,
  fetchCommissionSettings,
  updateCommissionSettings,
  fetchAdminQr,
  updateAdminQr,
  uploadAdminQrImage,
  CommissionSettlement,
  BankDetails,
} from "../api/commissionApi";

const PER_PAGE = 10;

const STAT_TABS: { id: string; label: string }[] = [
  { id: "all", label: "All" },
  { id: "pending", label: "Pending" },
  { id: "overdue", label: "Overdue" },
  { id: "submitted", label: "Submitted" },
  { id: "verified", label: "Verified" },
  { id: "rejected", label: "Rejected" },
];

const QR_TYPES: { key: keyof BankDetails; type: "esewa" | "khalti" | "bank"; label: string }[] = [
  { key: "esewa_qr_url", type: "esewa", label: "eSewa" },
  { key: "khalti_qr_url", type: "khalti", label: "Khalti" },
  { key: "bank_qr_url", type: "bank", label: "Bank Transfer" },
];

export function CommissionSettlementsPage() {
  const { showToast } = useToast();
  const [rows, setRows] = useState<CommissionSettlement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [tab, setTab] = useState("all");
  const [page, setPage] = useState(1);
  const [viewing, setViewing] = useState<CommissionSettlement | null>(null);
  const [processing, setProcessing] = useState<CommissionSettlement | null>(null);
  const [newStatus, setNewStatus] = useState<"verified" | "rejected">("verified");
  const [adminNotes, setAdminNotes] = useState("");
  // Edge case 3: what the cook actually paid, which is not always what they
  // owed. Pre-filled with the outstanding balance so the common "paid in full"
  // case is one click, but editable so a shortfall gets recorded instead of
  // silently written off.
  const [amountReceived, setAmountReceived] = useState("");
  // Edge case 4: a cook who paid by direct bank transfer never reaches
  // 'submitted', so there is no screenshot to verify. This records the payment
  // against a still-'pending' settlement; the backend makes admin_notes
  // mandatory on that path so the money always has a paper trail.
  const [recording, setRecording] = useState<CommissionSettlement | null>(null);
  const [recordNotes, setRecordNotes] = useState("");
  const [recordAmount, setRecordAmount] = useState("");
  const [saving, setSaving] = useState(false);
  const [generating, setGenerating] = useState(false);

  // Commission rate
  const [commissionPct, setCommissionPct] = useState<string>("");
  const [editingRate, setEditingRate] = useState(false);
  const [newRate, setNewRate] = useState<string>("");
  const [changeReason, setChangeReason] = useState<string>("");
  const [savingRate, setSavingRate] = useState(false);

  // Admin's own payment QR
  const [bankDetails, setBankDetails] = useState<BankDetails>({});
  const [uploadingQr, setUploadingQr] = useState<string | null>(null);
  const fileInputs = { esewa: useRef<HTMLInputElement>(null), khalti: useRef<HTMLInputElement>(null), bank: useRef<HTMLInputElement>(null) };

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await fetchSettlements();
      setRows(data);
    } catch (e: any) {
      setError(e?.message || "Failed to load settlements.");
    } finally {
      setLoading(false);
    }
  }

  async function loadSettingsAndQr() {
    try {
      const [settings, qr] = await Promise.all([fetchCommissionSettings(), fetchAdminQr()]);
      setCommissionPct(String(settings.commission_pct));
      setBankDetails(qr || {});
    } catch (e: any) {
      showToast(e?.message || "Failed to load commission settings.", "error");
    }
  }

  useEffect(() => { load(); loadSettingsAndQr(); }, []);
  useEffect(() => { setPage(1); }, [tab, search]);

  const filtered = rows.filter(r => {
    const s = !search
      || r.displayId.toLowerCase().includes(search.toLowerCase())
      || r.cook.toLowerCase().includes(search.toLowerCase())
      || r.period.toLowerCase().includes(search.toLowerCase());
    const t = tab === "all" || r.status === tab;
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible = filtered.slice((page - 1) * PER_PAGE, page * PER_PAGE);

  const startProcess = (r: CommissionSettlement) => {
    setProcessing(r);
    setNewStatus("verified");
    setAdminNotes("");
    setAmountReceived(r.amountRemaining.toFixed(2));
  };

  /**
   * Validates an entered payment amount against the outstanding balance,
   * mirroring the backend's checks so the admin gets an inline error rather
   * than a 400. Returns the parsed number, or null if the input is unusable
   * (the caller has already been shown a toast by then).
   */
  const parseAmount = (raw: string, r: CommissionSettlement): number | null => {
    const n = Number(raw);
    if (!raw.trim() || !Number.isFinite(n) || n <= 0) {
      showToast("Enter the amount received as a number greater than 0.", "error");
      return null;
    }
    // Compared in paisa — 145 - 100 - 45 is not reliably 0 in floating point.
    if (Math.round(n * 100) > Math.round(r.amountRemaining * 100)) {
      showToast(`That is more than the ₹${r.amountRemaining.toFixed(2)} still outstanding.`, "error");
      return null;
    }
    return n;
  };

  const submitProcess = async () => {
    if (!processing) return;
    // A rejection means no money arrived, so the amount field is irrelevant.
    let amount: number | undefined;
    if (newStatus === "verified") {
      const parsed = parseAmount(amountReceived, processing);
      if (parsed === null) return;
      amount = parsed;
    }
    setSaving(true);
    try {
      const res = await verifySettlementApi(processing.id, newStatus, adminNotes || undefined, amount);
      setProcessing(null);
      // The backend downgrades a short payment back to 'pending', so report what
      // it actually did rather than what was requested.
      showToast(res.message, "success");
      await load();
    } catch (e: any) {
      showToast(e?.message || "Failed to update settlement.", "error");
    } finally {
      setSaving(false);
    }
  };

  const startRecord = (r: CommissionSettlement) => {
    setRecording(r);
    setRecordNotes("");
    setRecordAmount(r.amountRemaining.toFixed(2));
  };

  const submitRecord = async () => {
    if (!recording) return;
    // Mirrors the backend's hard requirement rather than letting it 400 —
    // an off-platform payment with no explanation is untraceable money.
    if (!recordNotes.trim()) {
      showToast("Describe how the payment was received — this is required for off-platform payments.", "error");
      return;
    }
    const amount = parseAmount(recordAmount, recording);
    if (amount === null) return;
    setSaving(true);
    try {
      const res = await verifySettlementApi(recording.id, "verified", recordNotes.trim(), amount);
      setRecording(null);
      showToast(res.message, "success");
      await load();
    } catch (e: any) {
      showToast(e?.message || "Failed to record payment.", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleGenerate = async () => {    setGenerating(true);
    try {
      const { created } = await generateSettlementsApi();
      showToast(`Generated ${created} new settlement(s) for last month.`, "success");
      await load();
    } catch (e: any) {
      showToast(e?.message || "Failed to generate settlements.", "error");
    } finally {
      setGenerating(false);
    }
  };

  const handleSaveRate = async () => {
    const pct = Number(newRate);
    if (Number.isNaN(pct) || pct < 0 || pct > 100) {
      showToast("Commission % must be a number between 0 and 100.", "error");
      return;
    }
    setSavingRate(true);
    try {
      const result = await updateCommissionSettings(pct, changeReason || undefined);
      if (result.no_change) {
        showToast("Commission rate unchanged.", "info");
      } else {
        showToast(
          `Commission rate updated from ${result.old_rate}% to ${result.new_rate}%. ${result.notified_cooks} cooks notified via push notification and ${result.chats_sent} via chat message.`,
          "success"
        );
      }
      setCommissionPct(String(pct));
      setEditingRate(false);
      setChangeReason("");
      await load();
    } catch (e: any) {
      showToast(e?.message || "Failed to update commission rate.", "error");
    } finally {
      setSavingRate(false);
    }
  };

  const handleQrFileChange = async (type: "esewa" | "khalti" | "bank", key: keyof BankDetails, file: File | null) => {
    if (!file) return;
    setUploadingQr(type);
    try {
      const url = await uploadAdminQrImage(file, type);
      const next = { ...bankDetails, [key]: url };
      await updateAdminQr(next);
      setBankDetails(next);
      showToast(`${type} QR updated.`, "success");
    } catch (e: any) {
      showToast(e?.message || "Failed to upload QR image.", "error");
    } finally {
      setUploadingQr(null);
    }
  };

  // EC3: these must total what is still *owed*, not what was originally billed —
  // otherwise a part-paid settlement keeps inflating "Awaiting Payment" by the
  // full amount even after money has come in. Verified totals use amountPaid for
  // the same reason: it's the figure that reflects cash actually received.
  const totalPending = rows.filter(r => r.status === "pending" || r.status === "overdue")
    .reduce((sum, r) => sum + r.amountRemaining, 0);
  const totalSubmitted = rows.filter(r => r.status === "submitted").reduce((sum, r) => sum + r.amountRemaining, 0);
  const totalVerified = rows.filter(r => r.status === "verified").reduce((sum, r) => sum + r.amountPaid, 0);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 28, color: "#1c1f29" }}>Commission Settlements</p>
        <p style={{ fontFamily: "Inter", fontWeight: 400, fontSize: 14, color: "#9499a6", marginTop: 4 }}>
          Cooks pay their accrued commission into the QR below once a month, upload proof, and you verify it here.
          No merchant account required — this is tracked manually, same as refunds.
        </p>
      </div>

      {/* Summary tiles */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white flex items-center gap-4 p-5 rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
          <span style={{ fontSize: 28 }}>⏳</span>
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>₹{totalPending.toFixed(0)}</p>
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>Awaiting Payment</p>
          </div>
        </div>
        <div className="bg-white flex items-center gap-4 p-5 rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
          <span style={{ fontSize: 28 }}>📩</span>
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>₹{totalSubmitted.toFixed(0)}</p>
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>Awaiting Verification</p>
          </div>
        </div>
        <div className="bg-white flex items-center gap-4 p-5 rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
          <span style={{ fontSize: 28 }}>✅</span>
          <div>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 20, color: "#1c1f29" }}>₹{totalVerified.toFixed(0)}</p>
            <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>Verified This Period</p>
          </div>
        </div>
      </div>

      {/* Commission rate + generate */}
      <div className="bg-white rounded-[12px] p-4 sm:p-6 flex flex-col sm:flex-row sm:items-end gap-4" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
        <div style={{ flex: 1 }}>
          <p style={{ fontFamily: "Inter", fontWeight: 600, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>Platform Commission Rate</p>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 24, color: "#3b82f6" }}>{commissionPct}%</span>
            </div>
            <button 
              onClick={() => { setNewRate(commissionPct); setChangeReason(""); setEditingRate(true); }}
              style={{ background: "#3b82f6", border: "none", fontFamily: "Inter", fontWeight: 600, color: "white", fontSize: 13, padding: "10px 16px", borderRadius: 8, cursor: "pointer" }}>
              Update Rate
            </button>
          </div>
          <p style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6", marginTop: 6 }}>
            Applied to orders going forward — past orders keep their snapshotted rate.
          </p>
        </div>
        <div>
          <button onClick={handleGenerate} disabled={generating}
            style={{ background: "#7887fa", border: "none", fontFamily: "Inter", fontWeight: 600, color: "white", fontSize: 13, padding: "12px 18px", borderRadius: 8, cursor: generating ? "not-allowed" : "pointer", opacity: generating ? 0.7 : 1 }}>
            {generating ? "Generating…" : "Generate Last Month's Dues"}
          </button>
          <p style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6", marginTop: 6, maxWidth: 220 }}>Runs automatically on the 1st of each month — use this to run it early or re-check.</p>
        </div>
      </div>

      {/* Admin's own payment QR */}
      <div className="bg-white rounded-[12px] p-4 sm:p-6" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
        <p style={{ fontFamily: "Inter", fontWeight: 600, fontSize: 16, color: "#1c1f29", marginBottom: 4 }}>Platform Payment QR</p>
        <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", marginBottom: 16 }}>
          Shown to cooks when they pay their monthly commission. Upload the QR from your personal/business eSewa, Khalti, or bank app.
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {QR_TYPES.map(({ key, type, label }) => (
            <div key={type} className="flex flex-col items-center gap-3 p-4 rounded-[10px]" style={{ border: "1px solid #e5e8ed" }}>
              <p style={{ fontFamily: "Inter", fontWeight: 600, fontSize: 13, color: "#1c1f29" }}>{label}</p>
              {bankDetails[key] ? (
                <img src={bankDetails[key] as string} alt={`${label} QR`} style={{ width: 120, height: 120, objectFit: "contain", borderRadius: 8, border: "1px solid #f2f5f7" }} />
              ) : (
                <div style={{ width: 120, height: 120, borderRadius: 8, background: "#f7f8fa", display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <span style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6" }}>No QR yet</span>
                </div>
              )}
              <input
                ref={fileInputs[type]}
                type="file" accept="image/*" style={{ display: "none" }}
                onChange={e => handleQrFileChange(type, key, e.target.files?.[0] || null)}
              />
              <button
                onClick={() => fileInputs[type].current?.click()}
                disabled={uploadingQr === type}
                style={{ background: "#f2f5f7", border: "none", fontFamily: "Inter", fontWeight: 600, color: "#1c1f29", fontSize: 12, padding: "8px 14px", borderRadius: 8, cursor: uploadingQr === type ? "not-allowed" : "pointer", opacity: uploadingQr === type ? 0.7 : 1 }}
              >
                {uploadingQr === type ? "Uploading…" : bankDetails[key] ? "Replace" : "Upload"}
              </button>
            </div>
          ))}
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
          placeholder="Search by settlement ID, cook, or period..." value={search} onChange={e => setSearch(e.target.value)} />
      </div>

      {error && (
        <div className="px-4 py-3 rounded-[8px]" style={{ background: "rgba(242,89,89,0.08)", color: "#f25959", fontFamily: "Inter", fontSize: 13 }}>
          {error}
        </div>
      )}

      <div className="bg-white rounded-[12px]" style={{ boxShadow: "0px 2px 8px rgba(0,0,0,0.08)" }}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1000px]">
            <div className="flex gap-4 pb-4" style={{ borderBottom: "1px solid #e5e8ed" }}>
              <p style={{ width: 40, flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 12, color: "#9499a6" }}>S.N</p>
              {["Settlement ID", "Cook", "Period", "Orders", "Amount Due", "Status", "Generated", "Actions"].map((h, i) => (
                <p key={h} style={{ width: [120, 160, 110, 80, 110, 110, 100, 90][i], flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 12, color: "#9499a6" }}>{h}</p>
              ))}
            </div>
            {loading && (
              <p style={{ fontFamily: "Inter", fontSize: 14, color: "#9499a6", textAlign: "center", padding: "32px 0" }}>Loading settlements…</p>
            )}
            {!loading && visible.length === 0 && (
              <p style={{ fontFamily: "Inter", fontSize: 14, color: "#9499a6", textAlign: "center", padding: "32px 0" }}>No settlements found.</p>
            )}
            {!loading && visible.map((r, idx) => (
              <div key={r.id}>
                <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                  <p style={{ width: 40, flexShrink: 0, fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#9499a6" }}>{(page - 1) * PER_PAGE + idx + 1}</p>
                  <p style={{ width: 120, flexShrink: 0, fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#7887fa" }}>{r.displayId}</p>
                  <p style={{ width: 160, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>
                    {r.cook}
                    {r.cookDeleted && (
                      <span title="This cook deleted their account — the settlement record was preserved"
                        style={{ display: "block", fontSize: 11, color: "#d97706" }}>
                        account deleted
                      </span>
                    )}
                  </p>
                  <p style={{ width: 110, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>{r.period}</p>
                  <p style={{ width: 80, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#1c1f29" }}>{r.orderCount}</p>
                  <p style={{ width: 110, flexShrink: 0, fontFamily: "Inter", fontWeight: 600, fontSize: 13, color: "#1c1f29" }}>
                    ₹{r.amountDue.toFixed(0)}
                    {r.amountPaid > 0 && r.amountRemaining > 0 && (
                      <span title={`₹${r.amountPaid.toFixed(2)} received, ₹${r.amountRemaining.toFixed(2)} still owed`}
                        style={{ display: "block", fontWeight: 500, fontSize: 11, color: "#d97706" }}>
                        ₹{r.amountRemaining.toFixed(0)} left
                      </span>
                    )}
                  </p>
                  <div style={{ width: 110, flexShrink: 0 }}><StatusBadge status={r.status} /></div>
                  <p style={{ width: 100, flexShrink: 0, fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>{r.date}</p>
                  <div style={{ width: 90, flexShrink: 0, display: "flex", gap: 4 }}>
                    <button title="View" onClick={() => setViewing(r)}
                      style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: "rgba(120,135,250,0.10)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
                      <Eye size={14} color="#7887fa" />
                    </button>
                    {r.status === "submitted" && (
                      <button title="Verify / Reject" onClick={() => startProcess(r)}
                        style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: "rgba(16,185,129,0.10)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
                        <CheckCircle2 size={14} color="#10b981" />
                      </button>
                    )}
                    {r.rawStatus === "pending" && (
                      <button title="Record off-platform payment" onClick={() => startRecord(r)}
                        style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: "rgba(245,158,11,0.12)", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
                        <Wallet size={14} color="#d97706" />
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
        <Modal title="Settlement Details" onClose={() => setViewing(null)}>
          <DetailRow label="Settlement ID" value={<span style={{ color: "#7887fa", fontWeight: 600 }}>{viewing.displayId}</span>} />
          <DetailRow label="Cook / Kitchen" value={viewing.cook} />
          <DetailRow label="Cook Phone" value={viewing.cookPhone || "—"} />
          <DetailRow label="Period" value={viewing.period} />
          <DetailRow label="Orders This Period" value={viewing.orderCount} />
          <DetailRow label="Amount Due" value={<span style={{ fontWeight: 700 }}>₹{viewing.amountDue.toFixed(2)}</span>} />
          <DetailRow label="Received So Far" value={
            viewing.amountPaid > 0
              ? <span style={{ color: viewing.amountRemaining > 0 ? "#d97706" : "#10b981", fontWeight: 600 }}>
                  ₹{viewing.amountPaid.toFixed(2)}
                  {viewing.amountRemaining > 0 ? ` — ₹${viewing.amountRemaining.toFixed(2)} still outstanding` : " — paid in full"}
                </span>
              : "Nothing received yet"
          } />
          <DetailRow label="Status" value={<StatusBadge status={viewing.status} />} />
          <DetailRow label="Due Date" value={
            viewing.dueDate
              ? <span style={{ color: viewing.isOverdue ? "#f25959" : "#1c1f29", fontWeight: viewing.isOverdue ? 600 : 400 }}>
                  {viewing.dueDate}{viewing.isOverdue ? " — overdue" : ""}
                </span>
              : "Not set (generated before due dates were tracked)"
          } />
          <DetailRow label="Payment Screenshot" value={
            viewing.screenshotUrl
              ? <a href={viewing.screenshotUrl} target="_blank" rel="noreferrer" style={{ color: "#7887fa" }}>View screenshot ↗</a>
              : "Not submitted yet"
          } />
          <DetailRow label="Admin Notes" value={viewing.adminNotes || "—"} />
          <DetailRow label="Verified By" value={viewing.verifiedBy || "—"} />
          <DetailRow label="Generated On" value={viewing.date} />
        </Modal>
      )}

      {processing && (
        <Modal title={`Verify ${processing.displayId}`} onClose={() => setProcessing(null)}>
          <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", marginBottom: 16 }}>
            {processing.cook} submitted proof of payment for {processing.period}.
            {" "}<strong style={{ color: "#1c1f29" }}>₹{processing.amountRemaining.toFixed(2)}</strong> is outstanding
            {processing.amountPaid > 0 ? ` of ₹${processing.amountDue.toFixed(2)} billed (₹${processing.amountPaid.toFixed(2)} already received).` : "."}
          </p>
          {processing.screenshotUrl && (
            <a href={processing.screenshotUrl} target="_blank" rel="noreferrer">
              <img src={processing.screenshotUrl} alt="Payment proof" style={{ width: "100%", maxHeight: 260, objectFit: "contain", borderRadius: 8, border: "1px solid #e5e8ed", marginBottom: 16 }} />
            </a>
          )}
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>Decision</p>
            <select
              value={newStatus}
              onChange={e => setNewStatus(e.target.value as typeof newStatus)}
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", background: "white", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 14, outline: "none" }}
            >
              <option value="verified">Verify — payment confirmed</option>
              <option value="rejected">Reject — ask cook to re-upload</option>
            </select>
          </div>
          {newStatus === "verified" && (
            <div style={{ marginBottom: 16 }}>
              <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>Amount Received (₹)</p>
              <input
                type="number" min={0} step={0.01}
                value={amountReceived}
                onChange={e => setAmountReceived(e.target.value)}
                style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 14, outline: "none" }}
              />
              <p style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6", marginTop: 6 }}>
                Pre-filled with the ₹{processing.amountRemaining.toFixed(2)} outstanding. Lower it if the cook paid
                less — the shortfall is recorded and the settlement stays pending instead of being written off.
              </p>
            </div>
          )}
          <FormField label="Admin Notes (optional)" value={adminNotes} onChange={setAdminNotes} />
          <SaveCancel onCancel={() => setProcessing(null)} onSave={submitProcess} saving={saving} saveLabel="Update" />
        </Modal>
      )}

      {recording && (
        <Modal title={`Record Payment — ${recording.displayId}`} onClose={() => setRecording(null)}>
          <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", marginBottom: 12 }}>
            Use this when <strong style={{ color: "#1c1f29" }}>{recording.cook}</strong> paid their
            {" "}{recording.period} commission outside the app — direct bank transfer, cash, or a QR payment they
            never uploaded proof of. Recording the full outstanding balance marks the settlement verified without
            a screenshot; recording less leaves it pending for the remainder.
          </p>
          <div style={{ background: "rgba(245,158,11,0.08)", borderRadius: 8, padding: "10px 12px", marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontSize: 12, color: "#92400e" }}>
              There is no payment proof on file for this settlement. Your note below is the only record of how
              the money arrived, so it is required and is written to the admin audit log.
            </p>
          </div>
          {recording.dueDate && (
            <p style={{ fontFamily: "Inter", fontSize: 12, color: recording.isOverdue ? "#f25959" : "#9499a6", marginBottom: 16 }}>
              Due {recording.dueDate}{recording.isOverdue ? " — currently overdue" : ""}
            </p>
          )}
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>Amount Received (₹)</p>
            <input
              type="number" min={0} step={0.01}
              value={recordAmount}
              onChange={e => setRecordAmount(e.target.value)}
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 14, outline: "none" }}
            />
            <p style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6", marginTop: 6 }}>
              ₹{recording.amountRemaining.toFixed(2)} outstanding
              {recording.amountPaid > 0 ? ` (₹${recording.amountPaid.toFixed(2)} already received)` : ""}.
              A smaller amount is banked as a part payment and the settlement stays pending.
            </p>
          </div>
          <FormField label="How was this paid? (required)" value={recordNotes} onChange={setRecordNotes} />
          <SaveCancel onCancel={() => setRecording(null)} onSave={submitRecord} saving={saving} saveLabel="Record Payment" />
        </Modal>
      )}

      {/* Commission Rate Update Modal */}
      {editingRate && (
        <Modal title="Update Commission Rate" onClose={() => setEditingRate(false)}>
          <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", marginBottom: 16 }}>
            Changing the commission rate will:
          </p>
          <ul style={{ fontFamily: "Inter", fontSize: 13, color: "#6b7280", marginBottom: 16, paddingLeft: 20 }}>
            <li style={{ marginBottom: 6 }}>✅ Apply to all <strong>future orders</strong> starting now</li>
            <li style={{ marginBottom: 6 }}>✅ Send <strong>push notifications</strong> to all active cooks</li>
            <li style={{ marginBottom: 6 }}>✅ Send <strong>automated chat messages</strong> to all cooks with details</li>
            <li style={{ marginBottom: 6 }}>✅ Keep existing settlements at their original rates (no retroactive changes)</li>
          </ul>
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>
              Current Rate: <span style={{ color: "#3b82f6", fontWeight: 700 }}>{commissionPct}%</span>
            </p>
          </div>
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>New Commission Rate (%)</p>
            <input
              type="number" min={0} max={100} step={0.5}
              value={newRate}
              onChange={e => setNewRate(e.target.value)}
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 14, outline: "none" }}
              placeholder="e.g., 7.5"
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>Reason for Change (optional)</p>
            <textarea
              value={changeReason}
              onChange={e => setChangeReason(e.target.value)}
              rows={3}
              style={{ border: "1px solid #e5e8ed", fontFamily: "Inter", color: "#1c1f29", width: "100%", padding: "12px 16px", borderRadius: 8, fontSize: 13, outline: "none", resize: "vertical" }}
              placeholder="e.g., Adjusting for increased operational costs..."
            />
            <p style={{ fontFamily: "Inter", fontSize: 11, color: "#9499a6", marginTop: 6 }}>
              This will be included in the chat message sent to all cooks. Leave blank if no specific reason.
            </p>
          </div>
          <SaveCancel onCancel={() => setEditingRate(false)} onSave={handleSaveRate} saving={savingRate} saveLabel="Update & Notify All Cooks" />
        </Modal>
      )}
    </div>
  );
}
