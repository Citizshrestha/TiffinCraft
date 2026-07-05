import React, { useState, useEffect } from "react";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";

const PER_PAGE = 8;
interface Meal { id:number; name:string; cook:string; price:string; category:string; available:boolean; }

const SEED: Meal[] = [
  { id:1,  name:"Dal Makhani",       cook:"Anita's Kitchen", price:"₹180", category:"North Indian", available:true  },
  { id:2,  name:"Paneer Tikka",      cook:"Spice Route",     price:"₹220", category:"North Indian", available:true  },
  { id:3,  name:"Masala Dosa",       cook:"South Indian",    price:"₹120", category:"South Indian", available:true  },
  { id:4,  name:"Chole Bhature",     cook:"Delhi Delights",  price:"₹160", category:"North Indian", available:false },
  { id:5,  name:"Biryani",           cook:"Mumbai Spice",    price:"₹260", category:"Mughlai",      available:true  },
  { id:6,  name:"Butter Chicken",    cook:"Punjab Kitchen",  price:"₹240", category:"North Indian", available:true  },
  { id:7,  name:"Fish Curry",        cook:"Bengal Bites",    price:"₹280", category:"Bengali",      available:true  },
  { id:8,  name:"Rajma Chawal",      cook:"Healthy Meals",   price:"₹140", category:"North Indian", available:false },
  { id:9,  name:"Idli Sambar",       cook:"South Indian",    price:"₹100", category:"South Indian", available:true  },
  { id:10, name:"Shahi Paneer",      cook:"Anita's Kitchen", price:"₹200", category:"North Indian", available:true  },
  { id:11, name:"Matar Paneer",      cook:"Tasty Tiffins",   price:"₹190", category:"North Indian", available:false },
  { id:12, name:"Chicken Curry",     cook:"Mumbai Spice",    price:"₹270", category:"Mughlai",      available:true  },
];

const EM: Record<string,string> = {"North Indian":"🍛","South Indian":"🥘","Mughlai":"🍖","Bengali":"🐟"};
const CATS = ["North Indian","South Indian","Mughlai","Bengali","Continental","Chinese"];
const blankMeal: Omit<Meal,"id"> = { name:"", cook:"", price:"", category:"North Indian", available:true };

function validate(f: Omit<Meal,"id">): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.name.trim())  e.name  = "Meal name is required";
  if (!f.cook.trim())  e.cook  = "Cook / Kitchen is required";
  if (!f.price.trim()) e.price = "Price is required";
  return e;
}

export function MealsPage() {
  const [meals,   setMeals]   = useState<Meal[]>(SEED);
  const [search,  setSearch]  = useState("");
  const [catFilter,setCatFilter]=useState("all");
  const [page,    setPage]    = useState(1);
  const [viewing, setViewing] = useState<Meal|null>(null);
  const [editing, setEditing] = useState<Meal|null>(null);
  const [draft,   setDraft]   = useState<Meal|null>(null);
  const [editErrs,setEditErrs]= useState<Record<string,string>>({});
  const [del,     setDel]     = useState<Meal|null>(null);
  const [adding,  setAdding]  = useState(false);
  const [addForm, setAddForm] = useState<Omit<Meal,"id">>(blankMeal);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});

  const filtered   = meals.filter(m => {
    const s = !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.cook.toLowerCase().includes(search.toLowerCase());
    const c = catFilter==="all" || m.category===catFilter;
    return s && c;
  });
  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(()=>{ setPage(1); },[search,catFilter]);

  const toggle    = (id:number) => setMeals(p=>p.map(m=>m.id===id?{...m,available:!m.available}:m));
  const startEdit = (m:Meal)   => { setEditing(m); setDraft({...m}); setEditErrs({}); };
  const saveEdit  = ()         => {
    if(!draft) return;
    const errs=validate(draft);
    if(Object.keys(errs).length){setEditErrs(errs);return;}
    setMeals(p=>p.map(m=>m.id===draft.id?draft:m)); setEditing(null); setDraft(null); setEditErrs({});
  };
  const doDelete  = ()         => { if(!del) return; setMeals(p=>p.filter(m=>m.id!==del.id)); setDel(null); };
  const submitAdd = ()         => {
    const errs=validate(addForm);
    if(Object.keys(errs).length){setAddErrs(errs);return;}
    setMeals(p=>[...p,{...addForm,id:Date.now()}]);
    setAdding(false); setAddForm(blankMeal); setAddErrs({});
  };

  const ErrMsg = ({field,errs}:{field:string;errs:Record<string,string>}) =>
    errs[field]?<p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{errs[field]}</p>:null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Meals</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Browse and manage all available meals.</p>
        </div>
        <button onClick={()=>{setAdding(true);setAddForm(blankMeal);setAddErrs({});}}
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          + Add Meal
        </button>
      </div>

      <div className="flex gap-3 flex-wrap items-center">
        <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white flex-1" style={{border:"1px solid #e5e8ed",minWidth:200}}>
          <span>🔍</span>
          <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
            placeholder="Search meals..." value={search} onChange={e=>setSearch(e.target.value)}/>
        </div>
        <div className="flex gap-2 flex-wrap">
          {["all",...CATS].map(c=>(
            <button key={c} onClick={()=>setCatFilter(c)}
              style={{padding:"8px 14px",borderRadius:8,fontSize:12,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
                background:catFilter===c?"#57b869":"#f2f5f7",color:catFilter===c?"#fff":"#9499a6"}}>
              {c==="all"?"All":c}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
        {visible.map(m=>(
          <div key={m.id} className="bg-white rounded-[12px] overflow-hidden hover:shadow-md transition-shadow" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <div className="h-[120px] flex items-center justify-center" style={{background:"#f2f5f7"}}>
              <span style={{fontSize:48}}>{EM[m.category]||"🍛"}</span>
            </div>
            <div className="p-4">
              <p style={{fontFamily:"Inter",fontWeight:600,fontSize:15,color:"#1c1f29",marginBottom:4}}>{m.name}</p>
              <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",marginBottom:10}}>{m.cook}</p>
              <div className="flex items-center justify-between mb-3">
                <p style={{fontFamily:"Inter",fontWeight:700,fontSize:14,color:"#1c1f29"}}>{m.price}</p>
                <span style={{background:"#f2f5f7",fontFamily:"Inter",fontWeight:500,fontSize:11,color:"#9499a6",padding:"3px 8px",borderRadius:6}}>{m.category}</span>
              </div>
              <div className="flex items-center justify-between mb-3">
                <p style={{fontFamily:"Inter",fontSize:12,color:m.available?"#57b869":"#9499a6"}}>{m.available?"Available":"Unavailable"}</p>
                <button onClick={()=>toggle(m.id)} style={{width:40,height:20,borderRadius:10,border:"none",background:m.available?"#57b869":"#e5e8ed",position:"relative",cursor:"pointer",transition:"background 150ms"}}>
                  <div style={{position:"absolute",top:2,left:m.available?"calc(100% - 18px)":2,width:16,height:16,borderRadius:8,background:"white",transition:"left 150ms"}}/>
                </button>
              </div>
              <div className="flex justify-end">
                <ActionButtons onView={()=>setViewing(m)} onEdit={()=>startEdit(m)} onDelete={()=>setDel(m)}/>
              </div>
            </div>
          </div>
        ))}
        {visible.length===0&&(
          <div className="col-span-4 text-center py-12">
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6"}}>No meals found.</p>
          </div>
        )}
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {viewing&&(
        <Modal title="Meal Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:64,height:64,borderRadius:12,background:"#f2f5f7",display:"flex",alignItems:"center",justifyContent:"center",fontSize:36}}>{EM[viewing.category]||"🍛"}</div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.name}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{viewing.cook}</p>
            </div>
          </div>
          <DetailRow label="Price"        value={viewing.price}/>
          <DetailRow label="Category"     value={viewing.category}/>
          <DetailRow label="Availability" value={<span style={{color:viewing.available?"#57b869":"#9499a6",fontWeight:600}}>{viewing.available?"✓ Available":"✗ Unavailable"}</span>}/>
        </Modal>
      )}

      {editing&&draft&&(
        <Modal title="Edit Meal" onClose={()=>{setEditing(null);setDraft(null);setEditErrs({});}}>
          <FormField label="Meal Name"      value={draft.name}     onChange={v=>setDraft({...draft,name:v})}/>
          <ErrMsg field="name"  errs={editErrs}/>
          <FormField label="Cook / Kitchen" value={draft.cook}     onChange={v=>setDraft({...draft,cook:v})}/>
          <ErrMsg field="cook"  errs={editErrs}/>
          <FormField label="Price"          value={draft.price}    onChange={v=>setDraft({...draft,price:v})}/>
          <ErrMsg field="price" errs={editErrs}/>
          <FormField label="Category"       value={draft.category} onChange={v=>setDraft({...draft,category:v})} options={CATS}/>
          <FormField label="Availability"   value={draft.available?"available":"unavailable"} onChange={v=>setDraft({...draft,available:v==="available"})} options={["available","unavailable"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);setEditErrs({});}} onSave={saveEdit}/>
        </Modal>
      )}

      {adding&&(
        <Modal title="Add New Meal" onClose={()=>{setAdding(false);setAddErrs({});}}>
          <FormField label="Meal Name"      value={addForm.name}     onChange={v=>setAddForm({...addForm,name:v})}/>
          <ErrMsg field="name"  errs={addErrs}/>
          <FormField label="Cook / Kitchen" value={addForm.cook}     onChange={v=>setAddForm({...addForm,cook:v})}/>
          <ErrMsg field="cook"  errs={addErrs}/>
          <FormField label="Price (e.g. ₹180)" value={addForm.price} onChange={v=>setAddForm({...addForm,price:v})}/>
          <ErrMsg field="price" errs={addErrs}/>
          <FormField label="Category"       value={addForm.category} onChange={v=>setAddForm({...addForm,category:v})} options={CATS}/>
          <FormField label="Availability"   value={addForm.available?"available":"unavailable"} onChange={v=>setAddForm({...addForm,available:v==="available"})} options={["available","unavailable"]}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd}/>
        </Modal>
      )}

      {del&&<ConfirmDelete name={del.name} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
