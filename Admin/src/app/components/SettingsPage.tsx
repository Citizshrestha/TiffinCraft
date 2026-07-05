import React, { useState } from "react";

interface ProfileForm { name: string; email: string; phone: string; }
interface PasswordForm { current: string; newPass: string; confirm: string; }

export function SettingsPage() {
  // Profile state
  const [profile, setProfile]     = useState<ProfileForm>({ name:"Admin User", email:"admin@tiffincraft.com", phone:"+91 98765 43210" });
  const [profileErrs, setProfileErrs] = useState<Record<string,string>>({});
  const [profileSaved, setProfileSaved] = useState(false);

  // Password state
  const [pwd, setPwd]             = useState<PasswordForm>({ current:"", newPass:"", confirm:"" });
  const [pwdErrs, setPwdErrs]     = useState<Record<string,string>>({});
  const [pwdSaved, setPwdSaved]   = useState(false);

  // Notification toggles
  const [notifs, setNotifs] = useState({
    email:true, sms:false, push:true, newOrders:true, reviews:true, payments:false,
  });
  const toggleNotif = (k: keyof typeof notifs) => setNotifs(p=>({...p,[k]:!p[k]}));

  function validateProfile(): Record<string,string> {
    const e: Record<string,string> = {};
    if (!profile.name.trim())  e.name  = "Name is required";
    if (!profile.email.trim()) e.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profile.email)) e.email = "Invalid email address";
    if (profile.phone && !/^[+\d\s\-()]{7,}$/.test(profile.phone)) e.phone = "Invalid phone number";
    return e;
  }

  function saveProfile() {
    const errs = validateProfile();
    if (Object.keys(errs).length) { setProfileErrs(errs); setProfileSaved(false); return; }
    setProfileErrs({});
    setProfileSaved(true);
    setTimeout(() => setProfileSaved(false), 3000);
  }

  function validatePassword(): Record<string,string> {
    const e: Record<string,string> = {};
    if (!pwd.current.trim())    e.current = "Current password is required";
    if (!pwd.newPass.trim())    e.newPass = "New password is required";
    else if (pwd.newPass.length < 8) e.newPass = "Password must be at least 8 characters";
    if (!pwd.confirm.trim())    e.confirm = "Please confirm your password";
    else if (pwd.newPass !== pwd.confirm) e.confirm = "Passwords do not match";
    return e;
  }

  function updatePassword() {
    const errs = validatePassword();
    if (Object.keys(errs).length) { setPwdErrs(errs); setPwdSaved(false); return; }
    setPwdErrs({});
    setPwd({ current:"", newPass:"", confirm:"" });
    setPwdSaved(true);
    setTimeout(() => setPwdSaved(false), 3000);
  }

  function Field({ label, value, onChange, type="text", error }: {
    label:string; value:string; onChange:(v:string)=>void; type?:string; error?:string;
  }) {
    return (
      <div style={{marginBottom:16}}>
        <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29",marginBottom:6}}>{label}</p>
        <input type={type} value={value} onChange={e=>onChange(e.target.value)}
          className="w-full px-4 py-3 rounded-[8px] text-[14px] outline-none transition-all"
          style={{border:`1px solid ${error?"#f25959":"#e5e8ed"}`,fontFamily:"Inter",color:"#1c1f29"}}
          onFocus={e=>(e.target.style.borderColor=error?"#f25959":"#57b869")}
          onBlur={e=>(e.target.style.borderColor=error?"#f25959":"#e5e8ed")}/>
        {error && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:4}}>{error}</p>}
      </div>
    );
  }

  function Toggle({ on, onToggle }: { on:boolean; onToggle:()=>void }) {
    return (
      <button onClick={onToggle}
        className="w-10 h-5 rounded-full relative transition-colors duration-150 cursor-pointer shrink-0"
        style={{background:on?"#57b869":"#e5e8ed",border:"none"}}>
        <div className="absolute top-0.5 w-4 h-4 rounded-full bg-white transition-all duration-150"
          style={{left:on?"calc(100% - 18px)":2}}/>
      </button>
    );
  }

  function SuccessBanner({ msg }: { msg:string }) {
    return (
      <div style={{background:"rgba(87,184,105,0.1)",border:"1px solid rgba(87,184,105,0.3)",borderRadius:8,padding:"10px 16px",marginBottom:16}}>
        <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#57b869"}}>✓ {msg}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 max-w-2xl">
      <div>
        <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Settings</p>
        <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Manage your account and preferences.</p>
      </div>

      {/* Profile */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29",marginBottom:20}}>Profile</p>
        {profileSaved && <SuccessBanner msg="Profile updated successfully!"/>}
        <Field label="Full Name"     value={profile.name}  onChange={v=>setProfile({...profile,name:v})}  error={profileErrs.name}/>
        <Field label="Email Address" value={profile.email} onChange={v=>setProfile({...profile,email:v})} error={profileErrs.email} type="email"/>
        <Field label="Phone Number"  value={profile.phone} onChange={v=>setProfile({...profile,phone:v})} error={profileErrs.phone} type="tel"/>
        <button onClick={saveProfile}
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          Save Changes
        </button>
      </div>

      {/* Notifications */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29",marginBottom:20}}>Notifications</p>
        <div className="flex flex-col gap-4">
          {[
            { key:"email"     as const, label:"Email Notifications",  desc:"Receive updates via email"           },
            { key:"sms"       as const, label:"SMS Notifications",     desc:"Receive updates via SMS"             },
            { key:"push"      as const, label:"Push Notifications",    desc:"Browser push notifications"          },
            { key:"newOrders" as const, label:"New Orders",            desc:"Notify when new orders arrive"       },
            { key:"reviews"   as const, label:"New Reviews",           desc:"Notify when customers leave reviews" },
            { key:"payments"  as const, label:"Payment Alerts",        desc:"Notify on payment events"            },
          ].map((item,idx,arr)=>(
            <div key={item.key}>
              <div className="flex items-center justify-between py-2">
                <div>
                  <p style={{fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#1c1f29"}}>{item.label}</p>
                  <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{item.desc}</p>
                </div>
                <Toggle on={notifs[item.key]} onToggle={()=>toggleNotif(item.key)}/>
              </div>
              {idx<arr.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
        </div>
      </div>

      {/* Security */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29",marginBottom:20}}>Security</p>
        {pwdSaved && <SuccessBanner msg="Password updated successfully!"/>}
        <Field label="Current Password" value={pwd.current}  onChange={v=>setPwd({...pwd,current:v})}  error={pwdErrs.current}  type="password"/>
        <Field label="New Password"     value={pwd.newPass}  onChange={v=>setPwd({...pwd,newPass:v})}  error={pwdErrs.newPass}  type="password"/>
        <Field label="Confirm Password" value={pwd.confirm}  onChange={v=>setPwd({...pwd,confirm:v})}  error={pwdErrs.confirm}  type="password"/>
        <button onClick={updatePassword}
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          Update Password
        </button>
      </div>
    </div>
  );
}
