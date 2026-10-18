# 📊 Orbitwall Google Sheets Telemetry Setup Guide

Follow these quick steps to set up live, unified download tracking for Orbitwall:

### 1. Create a Google Sheet
- Open [Google Sheets](https://sheets.new) and create a new spreadsheet.
- Name it **Orbitwall Downloads**.

### 2. Add the Apps Script Code
- In the top menu, click **Extensions** > **Apps Script**.
- Delete any existing code in `Code.gs` and copy-paste the entire contents of [`Code.gs`](file:///C:/Users/Hp/.gemini/antigravity/scratch/Orbitwall/google_sheets_tracker/Code.gs).

### 3. Deploy as a Web App
- Click the blue **Deploy** button (top right) > **New deployment**.
- Click the gear icon next to "Select type" and choose **Web app**.
- Configure:
  - **Description:** `Orbitwall Telemetry`
  - **Execute as:** `Me (your email)`
  - **Who has access:** `Anyone` *(Crucial: This lets the Android app post download events without needing Google login)*
- Click **Deploy** and authorize access if prompted.
- Copy the generated **Web app URL** (it looks like `https://script.google.com/macros/s/.../exec`).

### 4. Activate in Orbitwall
- **Option A (Instant via Admin Console):** Open Orbitwall > Settings > Admin Console > Enter the Webhook URL > Click Save & Push.
- **Option B (Via GitHub repo):** Put the URL into `admin_config.json` under `"telemetry_url": "https://script.google.com/macros/s/.../exec"` and push.

Whenever any user worldwide downloads a wallpaper, your Google Sheet will automatically record the timestamp, title, and calculate total downloads in real time!
