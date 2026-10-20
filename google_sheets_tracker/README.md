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

### 4. (Optional) Auto-Commit to GitHub
If you want the Google Apps Script to automatically commit changes to `fahitakrim/Orbitwall/admin_config.json` on GitHub:
- In the Apps Script editor, click the **Project Settings** (gear icon on the left sidebar).
- Scroll down to **Script Properties** and click **Add script property**.
  - **Property:** `GITHUB_TOKEN`
  - **Value:** your GitHub personal access token (with `repo` write permission).
- Click **Save script properties**.

*(Note: Even without this token, the script still saves the admin configuration into your spreadsheet and serves it to all users instantly!)*

### 5. Activate in Orbitwall
- Open Orbitwall > Settings > Admin Console > **SYNC** tab.
- Paste your Web App URL into the **Google Apps Script Webhook URL** field and tap **SAVE WEBHOOK**.
- Now, whenever you tap **PUSH TO CLOUD**, all your Featured, Onboarding, and Ranking changes are broadcast live to all users worldwide with zero tokens in the app!

Whenever any user downloads a wallpaper, your Google Sheet will also record the timestamp, title, and calculate total downloads in real time!
