import React, { useState, useEffect, useCallback } from "react";
import { StatusBadge } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import {
  Cook,
  CookVerification,
  PendingCook,
  fetchCooks,
  fetchPendingCooks,
  approveCookApi,
  rejectCookApi,
  createCookApi,
  updateCookApi,
  deleteCookApi,
} from "../api/cooksApi";
import { useToast } from "./Toast";

const PER_PAGE = 10;

interface AddForm {
  kitchen: string;
  owner: string;
  email: string;
  phone: string;
  password: string;
  rating: number;
  status: CookVerification;
}

const blankAdd: AddForm = { kitchen: "", owner: "", email: "", phone: "", password: "", rating: 4.5, status: "unverified" };

function validateAdd(f: AddForm): Record<string, string> {
  const e: Record<string, string> = {};
  if (!f.kitchen.trim()) e.kitchen = "Kitchen name is required";
  if (!f.owner.trim()) e.owner = "Owner name is required";
  if (!f.email.trim()) e.email = "Email is required";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email)) e.email = "Invalid email";
  if (!f.phone.trim()) e.phone = "Phone is required";
  else if (!/^\d{10}$/.test(f.phone)) e.phone = "Phone must be 10 digits";
  if (!f.password.trim()) e.password = "Password is required";
  else if (f.password.length < 6) e.password = "Password must be at least 6 characters";
  if (f.rating < 0 || f.rating > 5) e.rating = "Rating must be between 0 and 5";
  return e;
}

function validateEdit(f: { kitchen: string; owner: string; rating: number }): Record<string, string> {
  const e: Record<string, string> = {};
  if (!f.kitchen.trim()) e.kitchen = "Kitchen name is required";
  if (!f.owner.trim()) e.owner = "Owner name is required";
  if (f.rating < 0 || f.rating > 5) e.rating = "Rating must be between 0 and 5";
  return e;
}

export function CooksPage() {
  const [rows, setRows] = useState<Cook[]>([]);
  const [pendingRows, setPendingRows] = useState<PendingCook[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [showPending, setShowPending] = useState(false);
  const { showToast } = useToast();

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [viewing, setViewing] = useState<Cook | null>(null);
  const [editing, setEditing] = useState<Cook | null>(null);
  const [draft, setDraft] = useState<Cook | null>(null);
  const [editErrs, setEditErrs] = useState<Record<string, string>>({});
  const [del, setDel] = useState<Cook | null>(null);
  const [adding, setAdding] = useState(false);
  const [addForm, setAddForm] = useState<AddForm>(blankAdd);
  const [addErrs, setAddErrs] = useState<Record<string, string>>({});
  const [savingEdit, setSavingEdit] = useState(false);
  const [savingAdd, setSavingAdd] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // ── Pending approval state ──────────────────────────
  const [viewPending, setViewPending] = useState<PendingCook | null>(null);
  const [rejectPending, setRejectPending] = useState<PendingCook | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [rejectSubmitting, setRejectSubmitting] = useState(false);
  const [approvingId, setApprovingId] = useState<number | null>(null);

  const loadCooks = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const data = await fetchCooks();
      setRows(data);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load cooks.");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadPendingCooks = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const data = await fetchPendingCooks();
      setPendingRows(data);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load pending cooks.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (showPending) {
      loadPendingCooks();
    } else {
      loadCooks();
    }
  }, [showPending, loadCooks, loadPendingCooks]);

  const filtered = rows.filter(c => !search || c.kitchen.toLowerCase().includes(search.toLowerCase()) || c.owner.toLowerCase().includes(search.toLowerCase()));
  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible = filtered.slice((page - 1) * PER_PAGE, page * PER_PAGE);

  useEffect(() => { setPage(1); }, [search]);

  const startEdit = (c: Cook) => { setEditing(c); setDraft({ ...c }); setEditErrs({}); };

  const saveEdit = async () => {
    if (!draft || savingEdit) return;
    const errs = validateEdit(draft);
    if (Object.keys(errs).length) { setEditErrs(errs); return; }
    setSavingEdit(true);
    setEditErrs({});
    try {
      const updated = await updateCookApi(draft.id, {
        full_name: draft.owner,
        kitchen_name: draft.kitchen,
        rating: draft.rating,
        is_verified: draft.status === "verified",
      });
      setRows(p => p.map(c => c.id === updated.id ? updated : c));
      setEditing(null); setDraft(null);
      showToast("Cook updated successfully", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to update cook.", "error");
    } finally {
      setSavingEdit(false);
    }
  };

  const doDelete = async () => {
    if (!del || deleting) return;
    setDeleting(true);
    try {
      await deleteCookApi(del.id);
      setRows(p => p.filter(c => c.id !== del.id));
      setDel(null);
      showToast("Cook deleted successfully", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to delete cook.", "error");
      setDel(null);
    } finally {
      setDeleting(false);
    }
  };

  const submitAdd = async () => {
    if (savingAdd) return;
    const errs = validateAdd(addForm);
    if (Object.keys(errs).length) { setAddErrs(errs); return; }
    setSavingAdd(true);
    setAddErrs({});
    try {
      const created = await createCookApi({
        full_name: addForm.owner,
        email: addForm.email,
        phone: addForm.phone,
        password: addForm.password,
        kitchen_name: addForm.kitchen,
        rating: addForm.rating,
        is_verified: addForm.status === "verified",
      });
      setRows(p => [created, ...p]);
      setAdding(false);
      setAddForm(blankAdd);
      showToast("Cook created successfully", "success");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create cook.";
      if (/email/i.test(message)) setAddErrs({ email: message });
      else if (/phone/i.test(message)) setAddErrs({ phone: message });
      else showToast(message, "error");
    } finally {
      setSavingAdd(false);
    }
  };

  // ── Pending cook actions ──────────────────────────────
  const handleApprove = async (c: PendingCook) => {
    setApprovingId(c.id);
    try {
      await approveCookApi(c.id);
      setPendingRows(p => p.filter(r => r.id !== c.id));
      showToast(`${c.kitchen} approved successfully`, "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to approve cook.", "error");
    } finally {
      setApprovingId(null);
    }
  };

  const handleReject = async () => {
    if (!rejectPending || rejectSubmitting) return;
    setRejectSubmitting(true);
    try {
      await rejectCookApi(rejectPending.id, rejectReason);
      setPendingRows(p => p.filter(r => r.id !== rejectPending.id));
      showToast(`${rejectPending.kitchen} rejected`, "success");
      setRejectPending(null);
      setRejectReason("");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to reject cook.", "error");
    } finally {
      setRejectSubmitting(false);
    }
  };

  const ErrMsg = ({ field, errs }: { field: string; errs: Record<string, string> }) =>
    errs[field] ? <p style={{ fontFamily: "Inter", fontSize: 11, color: "#f25959", marginTop: 2 }}>{errs[field]}</p> : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Cooks</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and manage all cook profiles.</p>
        </div>
        <button onClick={()=>{setAdding(true);setAddForm(blankAdd);setAddErrs({});}}
          className="self-start shrink-0"
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          + Add Cook
        </button>
      </div>

      {/* ── Tab toggle ───────────────────────────────────── */}
      <div className="flex gap-2">
        <button
          onClick={() => setShowPending(false)}
          style={{
            fontFamily:"Inter",fontWeight:600,fontSize:13,padding:"8px 18px",borderRadius:8,cursor:"pointer",
            border: showPending ? "1px solid #e5e8ed" : "none",
            background: showPending ? "white" : "#57b869",
            color: showPending ? "#1c1f29" : "white",
          }}
        >
          All Cooks
        </button>
        <button
          onClick={() => setShowPending(true)}
          style={{
            fontFamily:"Inter",fontWeight:600,fontSize:13,padding:"8px 18px",borderRadius:8,cursor:"pointer",
            border: showPending ? "none" : "1px solid #e5e8ed",
            background: showPending ? "#57b869" : "white",
            color: showPending ? "white" : "#1c1f29",
          }}
        >
          Pending Approvals
        </button>
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search cooks..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      {/* ── Pending approvals view ────────────────────────── */}
      {showPending ? (
        <div className="flex flex-col gap-4">
          {loading && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>Loading pending cooks...</p>
          )}
          {!loading && loadError && (
            <div style={{textAlign:"center",padding:"32px 0"}}>
              <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959",marginBottom:12}}>{loadError}</p>
              <button onClick={loadPendingCooks} style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"8px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
            </div>
          )}
          {!loading && !loadError && pendingRows.length === 0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No pending approvals.</p>
          )}
          {!loading && !loadError && pendingRows.map(c => (
            <div
              key={c.id}
              className="bg-white rounded-[12px] p-5 flex flex-col gap-3"
              style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)",border:"1px solid #e5e8ed"}}
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontWeight:700,fontSize:18}}>{c.kitchen}</p>
                  <p className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:13}}>by {c.owner}</p>
                </div>
                <StatusBadge status={c.status} />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                <div>
                  <span className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:12,fontWeight:500}}>Email</span>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontSize:13}}>{c.email}</p>
                </div>
                <div>
                  <span className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:12,fontWeight:500}}>Phone</span>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontSize:13}}>{c.phone}</p>
                </div>
                <div className="sm:col-span-2">
                  <span className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:12,fontWeight:500}}>Bio</span>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontSize:13}}>{c.bio || "—"}</p>
                </div>
                <div>
                  <span className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:12,fontWeight:500}}>Specialties</span>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontSize:13}}>{c.specialties || "—"}</p>
                </div>
                <div>
                  <span className="text-[#9499a6]" style={{fontFamily:"Inter",fontSize:12,fontWeight:500}}>Capacity</span>
                  <p className="text-[#1c1f29]" style={{fontFamily:"Inter",fontSize:13}}>{c.capacity > 0 ? `${c.capacity} meals/day` : "Not set"}</p>
                </div>
              </div>

              <div className="flex gap-3 mt-2">
                <button
                  onClick={() => handleApprove(c)}
                  disabled={approvingId === c.id}
                  style={{
                    background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",
                    fontSize:13,padding:"8px 20px",borderRadius:8,cursor: approvingId === c.id ? "not-allowed" : "pointer",
                    opacity: approvingId === c.id ? 0.7 : 1,
                  }}
                >
                  {approvingId === c.id ? "Approving..." : "Approve"}
                </button>
                <button
                  onClick={() => { setRejectPending(c); setRejectReason(""); }}
                  style={{
                    background:"transparent",border:"1px solid #f25959",fontFamily:"Inter",fontWeight:600,color:"#f25959",
                    fontSize:13,padding:"8px 20px",borderRadius:8,cursor:"pointer",
                  }}
                >
                  Reject
                </button>
                <button
                  onClick={() => setViewPending(c)}
                  style={{
                    background:"transparent",border:"1px solid #e5e8ed",fontFamily:"Inter",fontWeight:500,color:"#1c1f29",
                    fontSize:13,padding:"8px 20px",borderRadius:8,cursor:"pointer",
                  }}
                >
                  View Details
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <>
          <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <div className="p-4 sm:p-6 overflow-x-auto">
              <div className="min-w-[1010px]">
              <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
                {["Kitchen Name","Owner Name","Rating","Total Orders","Verification","Joined Date","Actions"].map((h,i)=>(
                  <p key={h} style={{width:[180,150,80,110,110,110,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
                ))}
              </div>
              {loading && (
                <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>Loading cooks...</p>
              )}
              {!loading && loadError && (
                <div style={{textAlign:"center",padding:"32px 0"}}>
                  <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959",marginBottom:12}}>{loadError}</p>
                  <button onClick={loadCooks} style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"8px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
                </div>
              )}
              {!loading && !loadError && visible.length===0&&<p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No cooks found.</p>}
              {!loading && !loadError && visible.map((c,idx)=>(
                <div key={c.id}>
                  <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                    <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                    <div style={{width:180,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                      <div style={{width:36,height:36,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                        <span style={{fontFamily:"Inter",fontWeight:700,fontSize:12,color:"#6b7280"}}>{c.kitchen.slice(0,2)}</span>
                      </div>
                      <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{c.kitchen}</p>
                    </div>
                    <p style={{width:150,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{c.owner}</p>
                    <div style={{width:80,flexShrink:0,display:"flex",alignItems:"center",gap:4}}>
                      <span>⭐</span><p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{c.rating.toFixed(1)}</p>
                    </div>
                    <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{c.orders}+ orders</p>
                    <div style={{width:110,flexShrink:0}}><StatusBadge status={c.status}/></div>
                    <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{c.joined}</p>
                    <div style={{width:110,flexShrink:0}}>
                      <ActionButtons onView={()=>setViewing(c)} onEdit={()=>startEdit(c)} onDelete={()=>setDel(c)}/>
                    </div>
                  </div>
                  {idx<visible.length-1&&<div style={{height:1,background:"#f2f5f7"}}/>}
                </div>
              ))}
              </div>
            </div>
          </div>

          <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>
        </>
      )}

      {viewing&&(
        <Modal title="Cook Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:64,height:64,borderRadius:"50%",background:"#f2f5f7",display:"flex",alignItems:"center",justifyContent:"center",fontSize:32}}>👨‍🍳</div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.kitchen}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>by {viewing.owner}</p>
            </div>
          </div>
          <DetailRow label="Owner"        value={viewing.owner}/>
          <DetailRow label="Email"        value={viewing.email}/>
          <DetailRow label="Phone"        value={viewing.phone}/>
          <DetailRow label="Rating"       value={`⭐ ${viewing.rating.toFixed(1)} / 5.0`}/>
          <DetailRow label="Total Orders" value={`${viewing.orders}+`}/>
          <DetailRow label="Verification" value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Joined"       value={viewing.joined}/>
        </Modal>
      )}

      {editing&&draft&&(
        <Modal title="Edit Cook" onClose={()=>{setEditing(null);setDraft(null);setEditErrs({});}}>
          <FormField label="Kitchen Name"  value={draft.kitchen} onChange={v=>setDraft({...draft,kitchen:v})}/>
          <ErrMsg field="kitchen" errs={editErrs}/>
          <FormField label="Owner Name"    value={draft.owner}   onChange={v=>setDraft({...draft,owner:v})}/>
          <ErrMsg field="owner"   errs={editErrs}/>
          <FormField label="Rating (0–5)"  value={String(draft.rating)} onChange={v=>setDraft({...draft,rating:parseFloat(v)||draft.rating})} type="number"/>
          <ErrMsg field="rating"  errs={editErrs}/>
          <FormField label="Verification"  value={draft.status}  onChange={v=>setDraft({...draft,status:v as CookVerification})} options={["verified","unverified"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);setEditErrs({});}} onSave={saveEdit} saving={savingEdit}/>
        </Modal>
      )}

      {adding&&(
        <Modal title="Add New Cook" onClose={()=>{setAdding(false);setAddErrs({});}}>
          <FormField label="Kitchen Name" value={addForm.kitchen} onChange={v=>setAddForm({...addForm,kitchen:v})}/>
          <ErrMsg field="kitchen" errs={addErrs}/>
          <FormField label="Owner Name"   value={addForm.owner}   onChange={v=>setAddForm({...addForm,owner:v})}/>
          <ErrMsg field="owner"   errs={addErrs}/>
          <FormField label="Email"        value={addForm.email}   onChange={v=>setAddForm({...addForm,email:v})} type="email"/>
          <ErrMsg field="email"   errs={addErrs}/>
          <FormField label="Phone"        value={addForm.phone}   onChange={v=>setAddForm({...addForm,phone:v})}/>
          <ErrMsg field="phone"   errs={addErrs}/>
          <FormField label="Password"     value={addForm.password} onChange={v=>setAddForm({...addForm,password:v})} type="password"/>
          <ErrMsg field="password" errs={addErrs}/>
          <FormField label="Initial Rating" value={String(addForm.rating)} onChange={v=>setAddForm({...addForm,rating:parseFloat(v)||4.0})} type="number"/>
          <ErrMsg field="rating"  errs={addErrs}/>
          <FormField label="Verification" value={addForm.status}  onChange={v=>setAddForm({...addForm,status:v as CookVerification})} options={["verified","unverified"]}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd} saving={savingAdd} saveLabel="Create Cook"/>
        </Modal>
      )}

      {del&&<ConfirmDelete name={del.kitchen} onConfirm={doDelete} onCancel={()=>setDel(null)} loading={deleting}/>}

      {/* ── View pending details modal ───────────────────── */}
      {viewPending && (
        <Modal title="Pending Cook Details" onClose={() => setViewPending(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:64,height:64,borderRadius:"50%",background:"#f2f5f7",display:"flex",alignItems:"center",justifyContent:"center",fontSize:32}}>👨‍🍳</div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewPending.kitchen}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>by {viewPending.owner}</p>
            </div>
          </div>
          <DetailRow label="Owner"        value={viewPending.owner}/>
          <DetailRow label="Email"        value={viewPending.email}/>
          <DetailRow label="Phone"        value={viewPending.phone}/>
          <DetailRow label="Bio"          value={viewPending.bio || "—"}/>
          <DetailRow label="Specialties"  value={viewPending.specialties || "—"}/>
          <DetailRow label="Capacity"     value={viewPending.capacity > 0 ? `${viewPending.capacity} meals/day` : "Not set"}/>
          <DetailRow label="Status"       value={<StatusBadge status={viewPending.status}/>}/>
        </Modal>
      )}

      {/* ── Reject reason modal ─────────────────────────── */}
      {rejectPending && (
        <Modal title={`Reject ${rejectPending.kitchen}`} onClose={() => { setRejectPending(null); setRejectReason(""); }}>
          <div className="flex flex-col gap-3">
            <label style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>Reason for rejection</label>
            <textarea
              className="w-full border rounded-[8px] p-3 outline-none resize-none"
              style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29",borderColor:"#e5e8ed",minHeight:100}}
              placeholder="Enter reason (optional)..."
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
            />
            <div className="flex gap-3 justify-end mt-2">
              <button
                onClick={() => { setRejectPending(null); setRejectReason(""); }}
                style={{
                  background:"transparent",border:"1px solid #e5e8ed",fontFamily:"Inter",fontWeight:500,color:"#1c1f29",
                  fontSize:13,padding:"8px 20px",borderRadius:8,cursor:"pointer",
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleReject}
                disabled={rejectSubmitting}
                style={{
                  background:"#f25959",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",
                  fontSize:13,padding:"8px 20px",borderRadius:8,cursor: rejectSubmitting ? "not-allowed" : "pointer",
                  opacity: rejectSubmitting ? 0.7 : 1,
                }}
              >
                {rejectSubmitting ? "Rejecting..." : "Reject"}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}