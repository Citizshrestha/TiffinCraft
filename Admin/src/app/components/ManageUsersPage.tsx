import React, { useState, useEffect, useCallback } from "react";
import { StatusBadge } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import {
  User,
  UStatus,
  fetchUsers,
  createUserApi,
  updateUserApi,
  deleteUserApi,
} from "../api/usersApi";
import { ApiError } from "../api/client";
import { toBackendRole } from "../utils/format";
import { useToast } from "./Toast";

const PER_PAGE = 10;

interface AddForm {
  name: string;
  email: string;
  phone: string;
  role: string;
  status: UStatus;
  password: string;
}

const blank: AddForm = { name: "", email: "", phone: "", role: "Customer", status: "active", password: "" };

const ini = (n: string) => n.split(" ").filter(Boolean).map(x => x[0]).join("").toUpperCase();

function validate(f: { name: string; email: string; phone: string }): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.name.trim())  e.name  = "Name is required";
  if (!f.email.trim()) e.email = "Email is required";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email)) e.email = "Invalid email";
  if (!f.phone.trim()) e.phone = "Phone is required";
  else if (!/^\d{10}$/.test(f.phone)) e.phone = "Phone must be 10 digits";
  return e;
}

function validateAdd(f: AddForm): Record<string,string> {
  const e = validate(f);
  if (!f.password.trim()) e.password = "Password is required";
  else if (f.password.length < 6) e.password = "Password must be at least 6 characters";
  return e;
}

export function ManageUsersPage() {
  const [rows,    setRows]    = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const { showToast } = useToast();

  const [tab,     setTab]     = useState("all");
  const [search,  setSearch]  = useState("");
  const [page,    setPage]    = useState(1);
  const [viewing, setViewing] = useState<User|null>(null);
  const [editing, setEditing] = useState<User|null>(null);
  const [draft,   setDraft]   = useState<User|null>(null);
  const [del,     setDel]     = useState<User|null>(null);
  const [adding,  setAdding]  = useState(false);
  const [addForm, setAddForm] = useState<AddForm>(blank);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});
  const [editErrs,setEditErrs]= useState<Record<string,string>>({});
  const [savingEdit, setSavingEdit] = useState(false);
  const [savingAdd,  setSavingAdd]  = useState(false);
  const [deleting,   setDeleting]   = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const data = await fetchUsers();
      setRows(data);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadUsers(); }, [loadUsers]);

  // Dynamic tab counts
  const counts = {
    all:       rows.length,
    customers: rows.filter(u => u.role === "Customer").length,
    cooks:     rows.filter(u => u.role === "Cook").length,
    admins:    rows.filter(u => u.role === "Admin").length,
  };

  const TABS = [
    { id:"all",       label:`All Users (${counts.all})`        },
    { id:"customers", label:`Customers (${counts.customers})`  },
    { id:"cooks",     label:`Cooks (${counts.cooks})`          },
    { id:"admins",    label:`Admins (${counts.admins})`        },
  ];

  const filtered = rows.filter(u => {
    const s = !search || u.name.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase());
    const t = tab==="all" || (tab==="customers"&&u.role==="Customer") || (tab==="cooks"&&u.role==="Cook") || (tab==="admins"&&u.role==="Admin");
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  // Reset to page 1 when filter changes
  useEffect(() => { setPage(1); }, [tab, search]);

  const startEdit = (u: User) => { setEditing(u); setDraft({...u}); setEditErrs({}); };

  const saveEdit = async () => {
    if (!draft || savingEdit) return;
    const errs = validate(draft);
    if (Object.keys(errs).length) { setEditErrs(errs); return; }
    setSavingEdit(true);
    setEditErrs({});
    try {
      const updated = await updateUserApi(draft.id, {
        full_name: draft.name,
        email: draft.email,
        phone: draft.phone,
        role: toBackendRole(draft.role),
        is_active: draft.status === "active",
      });
      setRows(p => p.map(u => u.id === updated.id ? updated : u));
      setEditing(null); setDraft(null);
      showToast("User updated successfully", "success");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to update user.";
      if (/email/i.test(message)) setEditErrs({ email: message });
      else if (/phone/i.test(message)) setEditErrs({ phone: message });
      else showToast(message, "error");
    } finally {
      setSavingEdit(false);
    }
  };

  const doDelete = async () => {
    if (!del || deleting) return;
    setDeleting(true);
    try {
      await deleteUserApi(del.id);
      setRows(p => p.filter(u => u.id !== del.id));
      setDel(null);
      showToast("User deleted successfully", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to delete user.", "error");
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
      const created = await createUserApi({
        full_name: addForm.name,
        email: addForm.email,
        phone: addForm.phone,
        role: toBackendRole(addForm.role),
        password: addForm.password,
        is_active: addForm.status === "active",
      });
      setRows(p => [created, ...p]);
      setAdding(false);
      setAddForm(blank);
      showToast("User created successfully", "success");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create user.";
      if (/email/i.test(message)) setAddErrs({ email: message });
      else if (/phone/i.test(message)) setAddErrs({ phone: message });
      else showToast(message, "error");
    } finally {
      setSavingAdd(false);
    }
  };

  const ErrMsg = ({ field, errs }: { field:string; errs:Record<string,string> }) =>
    errs[field] ? <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{errs[field]}</p> : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Users</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and manage all user data.</p>
        </div>
        <button onClick={()=>{setAdding(true);setAddForm(blank);setAddErrs({});}}
          className="self-start shrink-0"
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          + Add User
        </button>
      </div>

      <div className="flex gap-2 flex-wrap">
        {TABS.map(t=>(
          <button key={t.id} onClick={()=>setTab(t.id)}
            style={{padding:"10px 16px",borderRadius:8,fontSize:13,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
              background:tab===t.id?"#57b869":"#f2f5f7",color:tab===t.id?"#fff":"#9499a6"}}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search users..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[980px]">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Name","Role","Phone","Status","Joined On","Actions"].map((h,i)=>(
              <p key={h} style={{width:[240,120,140,100,120,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {loading && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>Loading users...</p>
          )}
          {!loading && loadError && (
            <div style={{textAlign:"center",padding:"32px 0"}}>
              <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959",marginBottom:12}}>{loadError}</p>
              <button onClick={loadUsers} style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"8px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
            </div>
          )}
          {!loading && !loadError && visible.length === 0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No users found.</p>
          )}
          {!loading && !loadError && visible.map((u,idx)=>(
            <div key={u.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <div style={{width:240,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                  {u.profileImage ? (
                    <img src={u.profileImage} alt={u.name}
                      style={{width:40,height:40,borderRadius:"50%",objectFit:"cover",flexShrink:0}}
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        e.currentTarget.nextElementSibling?.removeAttribute('style');
                      }}
                    />
                  ) : null}
                  <div style={{width:40,height:40,borderRadius:"50%",background:"#D9DEE6",display:u.profileImage?"none":"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{fontFamily:"Inter",fontWeight:700,fontSize:13,color:"#6b7280"}}>{ini(u.name)}</span>
                  </div>
                  <div>
                    <p style={{fontFamily:"Inter",fontWeight:600,fontSize:14,color:"#1c1f29"}}>{u.name}</p>
                    <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{u.email}</p>
                  </div>
                </div>
                <p style={{width:120,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{u.role}</p>
                <p style={{width:140,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{u.phone}</p>
                <div style={{width:100,flexShrink:0}}><StatusBadge status={u.status}/></div>
                <p style={{width:120,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{u.joined}</p>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons onView={()=>setViewing(u)} onEdit={()=>startEdit(u)} onDelete={()=>setDel(u)}/>
                </div>
              </div>
              {idx<visible.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
          </div>
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {/* View */}
      {viewing && (
        <Modal title="User Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            {viewing.profileImage ? (
              <img src={viewing.profileImage} alt={viewing.name}
                style={{width:64,height:64,borderRadius:"50%",objectFit:"cover"}}
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                  e.currentTarget.nextElementSibling?.removeAttribute('style');
                }}
              />
            ) : null}
            <div style={{width:64,height:64,borderRadius:"50%",background:"#D9DEE6",display:viewing.profileImage?"none":"flex",alignItems:"center",justifyContent:"center"}}>
              <span style={{fontFamily:"Inter",fontWeight:700,fontSize:20,color:"#6b7280"}}>{ini(viewing.name)}</span>
            </div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.name}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{viewing.email}</p>
            </div>
          </div>
          <DetailRow label="Role"      value={viewing.role}/>
          <DetailRow label="Phone"     value={viewing.phone}/>
          <DetailRow label="Status"    value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Joined On" value={viewing.joined}/>
        </Modal>
      )}

      {/* Edit */}
      {editing && draft && (
        <Modal title="Edit User" onClose={()=>{setEditing(null);setDraft(null);setEditErrs({});}}>
          {editErrs.general && <p style={{fontFamily:"Inter",fontSize:13,color:"#f25959",marginBottom:12}}>{editErrs.general}</p>}
          <FormField label="Full Name" value={draft.name}   onChange={v=>setDraft({...draft,name:v})}/>
          <ErrMsg field="name"  errs={editErrs}/>
          <FormField label="Email"     value={draft.email}  onChange={v=>setDraft({...draft,email:v})} type="email"/>
          <ErrMsg field="email" errs={editErrs}/>
          <FormField label="Phone"     value={draft.phone}  onChange={v=>setDraft({...draft,phone:v})}/>
          <ErrMsg field="phone" errs={editErrs}/>
          <FormField label="Role"      value={draft.role}   onChange={v=>setDraft({...draft,role:v})} options={["Customer","Cook","Admin"]}/>
          <FormField label="Status"    value={draft.status} onChange={v=>setDraft({...draft,status:v as UStatus})} options={["active","inactive"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);setEditErrs({});}} onSave={saveEdit} saving={savingEdit}/>
        </Modal>
      )}

      {/* Add */}
      {adding && (
        <Modal title="Add New User" onClose={()=>{setAdding(false);setAddErrs({});}}>
          {addErrs.general && <p style={{fontFamily:"Inter",fontSize:13,color:"#f25959",marginBottom:12}}>{addErrs.general}</p>}
          <FormField label="Full Name" value={addForm.name}   onChange={v=>setAddForm({...addForm,name:v})}/>
          <ErrMsg field="name"  errs={addErrs}/>
          <FormField label="Email"     value={addForm.email}  onChange={v=>setAddForm({...addForm,email:v})} type="email"/>
          <ErrMsg field="email" errs={addErrs}/>
          <FormField label="Phone"     value={addForm.phone}  onChange={v=>setAddForm({...addForm,phone:v})}/>
          <ErrMsg field="phone" errs={addErrs}/>
          <FormField label="Password"  value={addForm.password} onChange={v=>setAddForm({...addForm,password:v})} type="password"/>
          <ErrMsg field="password" errs={addErrs}/>
          <FormField label="Role"      value={addForm.role}   onChange={v=>setAddForm({...addForm,role:v})} options={["Customer","Cook","Admin"]}/>
          <FormField label="Status"    value={addForm.status} onChange={v=>setAddForm({...addForm,status:v as UStatus})} options={["active","inactive"]}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd} saving={savingAdd} saveLabel="Create User"/>
        </Modal>
      )}

      {del && <ConfirmDelete name={del.name} onConfirm={doDelete} onCancel={()=>setDel(null)} loading={deleting}/>}
    </div>
  );
}
