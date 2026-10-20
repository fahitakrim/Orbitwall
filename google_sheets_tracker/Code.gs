/**
 * Orbitwall Unified Download Tracker & Global Admin Cloud Sync
 * Google Apps Script Web App
 *
 * Capabilities:
 * 1. Download Telemetry: Records downloads into "Logs" & "Stats" sheets in real-time.
 * 2. Zero-Token Admin Cloud Sync: Admin in Android app can push Featured/Onboarding/Rankings
 *    with 1-click. The script verifies admin authentication and commits to GitHub & the Sheet.
 *
 * Setup:
 * 1. Create a new Google Sheet (e.g. "Orbitwall Backend").
 * 2. Extensions > Apps Script.
 * 3. Paste this code.
 * 4. (Optional for GitHub Auto-Commit): In Project Settings > Script Properties,
 *    add "GITHUB_TOKEN" with your GitHub token.
 * 5. Deploy > New deployment > Type: Web app > Who has access: Anyone.
 * 6. Copy Web app URL and paste into Orbitwall Settings > Admin Console > Sync.
 */

var GITHUB_OWNER = "fahitakrim";
var GITHUB_REPO = "Orbitwall";
var ADMIN_PASS_HASH = "e18f7fde6083e03b52cd162d4457252ac97d2cb312f8f22c0d9bc94739b1f850"; // SHA-256 of admin passcode

function doPost(e) {
  try {
    var rawData = e.postData.contents;
    var data = JSON.parse(rawData);
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // ==========================================
    // ACTION 1: Admin Cloud Sync (Zero Token from App)
    // ==========================================
    if (data.action === "sync_admin_config") {
      var passHash = hashSha256(data.admin_passcode || "");
      if (passHash !== ADMIN_PASS_HASH) {
        return createJsonResponse({ status: "error", message: "Unauthorized: Invalid admin credentials." });
      }

      var config = data.config || {};
      var now = new Date();
      config.last_updated = now.toISOString();

      // 1. Save config into "Config" sheet in Google Spreadsheet
      var configSheet = ss.getSheetByName("Config");
      if (!configSheet) {
        configSheet = ss.insertSheet("Config");
        configSheet.appendRow(["Key", "Value", "Updated At"]);
        configSheet.getRange(1, 1, 1, 3).setFontWeight("bold");
      }

      var configJson = JSON.stringify(config, null, 2);
      var configRange = configSheet.getDataRange().getValues();
      var foundRow = -1;
      for (var r = 1; r < configRange.length; r++) {
        if (configRange[r][0] === "admin_config") {
          foundRow = r + 1;
          break;
        }
      }

      if (foundRow > 0) {
        configSheet.getRange(foundRow, 2).setValue(configJson);
        configSheet.getRange(foundRow, 3).setValue(now);
      } else {
        configSheet.appendRow(["admin_config", configJson, now]);
      }

      // 2. Commit to GitHub admin_config.json if GITHUB_TOKEN is set in Script Properties
      var ghToken = PropertiesService.getScriptProperties().getProperty("GITHUB_TOKEN");
      var ghSynced = false;
      var ghMessage = "";

      if (ghToken && ghToken.length > 5) {
        try {
          var ghResult = commitToGitHub(ghToken, configJson);
          ghSynced = true;
          ghMessage = " (and committed to GitHub main branch)";
        } catch (ghErr) {
          ghMessage = " (Note: GitHub sync error: " + ghErr.toString() + ")";
        }
      }

      return createJsonResponse({
        status: "success",
        message: "Admin config synced to Cloud successfully!" + ghMessage,
        last_updated: config.last_updated
      });
    }

    // ==========================================
    // ACTION 2: Download Telemetry Logging
    // ==========================================
    var logSheet = ss.getSheetByName("Logs");
    if (!logSheet) {
      logSheet = ss.insertSheet("Logs");
      logSheet.appendRow(["Timestamp", "Wallpaper ID", "Title", "App", "Client Date"]);
      logSheet.getRange(1, 1, 1, 5).setFontWeight("bold");
    }

    var now = new Date();
    var clientDate = data.timestamp ? new Date(data.timestamp) : now;
    logSheet.appendRow([now, data.id || "unknown", data.title || "Untitled", data.app || "Orbitwall", clientDate]);

    var statsSheet = ss.getSheetByName("Stats");
    if (!statsSheet) {
      statsSheet = ss.insertSheet("Stats");
      statsSheet.appendRow(["Wallpaper ID", "Title", "Downloads", "Last Downloaded"]);
      statsSheet.getRange(1, 1, 1, 4).setFontWeight("bold");
    }

    var wallpaperId = data.id || "unknown";
    var title = data.title || "Untitled";
    var dataRange = statsSheet.getDataRange();
    var values = dataRange.getValues();
    var foundRow = -1;
    var currentCount = 0;

    for (var i = 1; i < values.length; i++) {
      if (values[i][0] == wallpaperId) {
        foundRow = i + 1;
        currentCount = Number(values[i][2]) || 0;
        break;
      }
    }

    var newCount = currentCount + 1;
    if (foundRow > 0) {
      statsSheet.getRange(foundRow, 2).setValue(title);
      statsSheet.getRange(foundRow, 3).setValue(newCount);
      statsSheet.getRange(foundRow, 4).setValue(now);
    } else {
      statsSheet.appendRow([wallpaperId, title, 1, now]);
    }

    var totalDownloads = 0;
    var updatedValues = statsSheet.getDataRange().getValues();
    for (var j = 1; j < updatedValues.length; j++) {
      totalDownloads += Number(updatedValues[j][2]) || 0;
    }

    return createJsonResponse({
      status: "success",
      id: wallpaperId,
      downloads: newCount,
      total_downloads: totalDownloads
    });

  } catch (err) {
    return createJsonResponse({ status: "error", message: err.toString() });
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "stats";

    // Action A: Serve live admin config
    if (action === "config") {
      var configSheet = ss.getSheetByName("Config");
      if (configSheet) {
        var values = configSheet.getDataRange().getValues();
        for (var i = 1; i < values.length; i++) {
          if (values[i][0] === "admin_config") {
            var rawJson = values[i][1];
            return ContentService.createTextOutput(rawJson).setMimeType(ContentService.MimeType.JSON);
          }
        }
      }
      return createJsonResponse({ status: "empty", message: "No custom config found" });
    }

    // Action B: Serve download stats
    var statsSheet = ss.getSheetByName("Stats");
    var downloads = {};
    var totalDownloads = 0;

    if (statsSheet) {
      var values = statsSheet.getDataRange().getValues();
      for (var i = 1; i < values.length; i++) {
        var id = values[i][0];
        var count = Number(values[i][2]) || 0;
        if (id) {
          downloads[id] = count;
          totalDownloads += count;
        }
      }
    }

    return createJsonResponse({
      status: "success",
      total_downloads: totalDownloads,
      downloads: downloads
    });

  } catch (err) {
    return createJsonResponse({ status: "error", message: err.toString() });
  }
}

function createJsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function hashSha256(str) {
  var rawHash = Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, str, Utilities.Charset.UTF_8);
  var hex = "";
  for (var i = 0; i < rawHash.length; i++) {
    var byteVal = rawHash[i];
    if (byteVal < 0) byteVal += 256;
    var byteHex = byteVal.toString(16);
    if (byteHex.length === 1) byteHex = "0" + byteHex;
    hex += byteHex;
  }
  return hex.toLowerCase();
}

function commitToGitHub(token, contentStr) {
  var path = "admin_config.json";
  var url = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/contents/" + path;

  // 1. Get current SHA
  var sha = null;
  try {
    var getResp = UrlFetchApp.fetch(url, {
      method: "get",
      headers: {
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "Google-Apps-Script"
      },
      muteHttpExceptions: true
    });
    if (getResp.getResponseCode() === 200) {
      var getJson = JSON.parse(getResp.getContentText());
      sha = getJson.sha;
    }
  } catch (e) {}

  // 2. PUT updated file
  var base64Content = Utilities.base64Encode(contentStr, Utilities.Charset.UTF_8);
  var payload = {
    message: "chore: sync admin_config from Orbitwall Cloud",
    content: base64Content,
    branch: "main"
  };
  if (sha) payload.sha = sha;

  var putResp = UrlFetchApp.fetch(url, {
    method: "put",
    contentType: "application/json",
    headers: {
      "Authorization": "Bearer " + token,
      "Accept": "application/vnd.github.v3+json",
      "User-Agent": "Google-Apps-Script"
    },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  });

  if (putResp.getResponseCode() >= 400) {
    throw new Error("GitHub API Error " + putResp.getResponseCode() + ": " + putResp.getContentText());
  }

  return true;
}
