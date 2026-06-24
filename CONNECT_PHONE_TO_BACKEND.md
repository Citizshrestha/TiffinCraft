# 📱 Connect Android App to Backend - Complete Guide

## ✅ What I Just Fixed

Updated the API endpoint in your Android app to match your current IP address:
- **Old IP:** `http://192.168.100.115:5000/api/`
- **New IP:** `http://192.168.100.208:5000/api/`
- **APK Rebuilt:** ✅ Ready to install

---

## 🚀 Next Steps

### 1. Install the New APK

Transfer and install the updated APK:
```
Location: C:\Users\ASUS\TiffinCraft\frontend\app\build\outputs\apk\debug\app-debug.apk
```

**Installation Methods:**
- Copy to phone via USB
- Email to yourself and download on phone
- Use Bluetooth/Nearby Share
- Upload to cloud (Google Drive/Dropbox)

### 2. Make Sure Backend is Running

```powershell
cd C:\Users\ASUS\TiffinCraft\backend
npm run dev
```

You should see:
```
✅ Server running on port 5000
🌐 Local: http://localhost:5000
🌐 Network: http://0.0.0.0:5000
MySQL Connected Successfully
```

### 3. Check Windows Firewall (Important!)

If connection still times out, you may need to allow the backend through Windows Firewall:

**Option A: Allow Node.js through firewall**
```powershell
# Run PowerShell as Administrator
New-NetFirewallRule -DisplayName "TiffinCraft Backend" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow
```

**Option B: Temporarily disable firewall for testing**
1. Open Windows Security
2. Firewall & network protection
3. Turn off for Private networks (temporarily)
4. Test the app
5. Turn it back on

### 4. Verify Connectivity

**From your PC:**
```powershell
# Check if backend responds
curl http://192.168.100.208:5000/api/health
```

**From your phone's browser:**
1. Open Chrome/Browser on your phone
2. Go to: `http://192.168.100.208:5000/api/health`
3. You should see: `{"status":"ok","database":"connected",...}`

If you can't see this, the issue is firewall or network connectivity.

---

## 🔍 Troubleshooting

### Issue: "Connection Timeout"

**Checklist:**
1. ✅ Backend is running on PC
2. ✅ Phone and PC are on the SAME WiFi network
3. ❓ Windows Firewall is allowing port 5000
4. ❓ Phone can reach PC's IP address

**Test Network Connectivity:**

From your phone, try pinging your PC:
- Install "Ping Tools" app from Play Store
- Ping: `192.168.100.208`
- If ping fails, network issue exists

### Issue: "Cannot Reach Server"

This means your phone can't find the PC at all.

**Possible causes:**
1. Phone and PC on different WiFi networks
2. PC's IP address changed (get new IP with `ipconfig`)
3. Router has AP isolation enabled (check router settings)

### Issue: IP Address Changed

If your PC's IP address changes (dynamic IP), you need to:

1. **Get new IP:**
   ```powershell
   ipconfig
   ```
   Look for "IPv4 Address" under your active network

2. **Update the app:**
   - Edit `RetrofitClient.java`
   - Change `BASE_URL` to new IP
   - Rebuild APK: `.\gradlew.bat assembleDebug`
   - Reinstall on phone

**Better Solution: Use Static IP**

Set a static IP for your PC in router settings:
1. Login to your router (usually 192.168.1.1 or 192.168.0.1)
2. Find DHCP settings
3. Reserve IP `192.168.100.208` for your PC's MAC address
4. Now your IP won't change!

---

## 📊 Current Configuration

| Component | Address |
|-----------|---------|
| Backend Server | http://192.168.100.208:5000 |
| API Base URL | http://192.168.100.208:5000/api/ |
| Health Check | http://192.168.100.208:5000/api/health |
| Database | localhost:3306 (MySQL) |

---

## 🎯 Quick Test Commands

**Check backend is running:**
```powershell
curl http://localhost:5000/api/health
```

**Check backend is accessible from network:**
```powershell
curl http://192.168.100.208:5000/api/health
```

**Check your current IP:**
```powershell
ipconfig | findstr "IPv4"
```

**Check what's using port 5000:**
```powershell
netstat -ano | findstr :5000
```

**Allow port 5000 through firewall (run as Admin):**
```powershell
New-NetFirewallRule -DisplayName "TiffinCraft Backend" -Direction Inbound -Protocol TCP -LocalPort 5000 -Action Allow
```

---

## 🔐 Security Note

The app is connecting over HTTP (not HTTPS) which is fine for development on your local network. For production, you'd want to:
- Use HTTPS
- Deploy to a proper server
- Not expose your backend directly

---

## ✅ Success Indicators

You'll know everything works when:
1. Backend shows: `✅ Server running on port 5000`
2. Health check in browser works: `http://192.168.100.208:5000/api/health`
3. Phone can open that URL in browser
4. App login doesn't timeout

**If all these work, you're ready to use the app!** 🎉
