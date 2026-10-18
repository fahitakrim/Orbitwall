/**
 * Orbitwall Unified Download Tracker & Analytics
 * Google Apps Script Web App
 *
 * Setup:
 * 1. Create a new Google Sheet (e.g. named "Orbitwall Downloads").
 * 2. Click Extensions > Apps Script.
 * 3. Replace all code with this file.
 * 4. Click Deploy > New deployment > Select type "Web app".
 * 5. Configuration:
 *    - Description: Orbitwall Telemetry
 *    - Execute as: Me
 *    - Who has access: Anyone (Important: so the Android app can send pings without authentication!)
 * 6. Click Deploy and copy the Web App URL.
 * 7. In Orbitwall Admin Console (or admin_config.json), set this URL as "Telemetry Webhook URL".
 */

function doPost(e) {
  try {
    var rawData = e.postData.contents;
    var data = JSON.parse(rawData);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // 1. Log to "Logs" sheet
    var logSheet = ss.getSheetByName("Logs");
    if (!logSheet) {
      logSheet = ss.insertSheet("Logs");
      logSheet.appendRow(["Timestamp", "Wallpaper ID", "Title", "App", "Client Date"]);
      logSheet.getRange(1, 1, 1, 5).setFontWeight("bold");
    }
    
    var now = new Date();
    var clientDate = data.timestamp ? new Date(data.timestamp) : now;
    logSheet.appendRow([now, data.id || "unknown", data.title || "Untitled", data.app || "Orbitwall", clientDate]);
    
    // 2. Update "Stats" sheet
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
    
    // Calculate total downloads
    var totalDownloads = 0;
    var updatedValues = statsSheet.getDataRange().getValues();
    for (var j = 1; j < updatedValues.length; j++) {
      totalDownloads += Number(updatedValues[j][2]) || 0;
    }
    
    var response = {
      status: "success",
      id: wallpaperId,
      downloads: newCount,
      total_downloads: totalDownloads
    };
    
    return ContentService
      .createTextOutput(JSON.stringify(response))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
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
    
    var response = {
      status: "success",
      total_downloads: totalDownloads,
      downloads: downloads
    };
    
    return ContentService
      .createTextOutput(JSON.stringify(response))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (err) {
    return ContentService
      .createTextOutput(JSON.stringify({ status: "error", message: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
