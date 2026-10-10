const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Retrieve ADMIN_TOKEN or ADMIN_TOKENS from environment variables
function getAdminTokens() {
  const tokenList = [];
  if (process.env.ADMIN_TOKEN) {
    tokenList.push(process.env.ADMIN_TOKEN.trim());
  }
  if (process.env.ADMIN_TOKENS) {
    process.env.ADMIN_TOKENS.split(",").forEach((t) => {
      const trimmed = t.trim();
      if (trimmed) tokenList.push(trimmed);
    });
  }
  return tokenList;
}

// Verify incoming request token against configured admin tokens
function verifyAdminToken(req) {
  const validTokens = getAdminTokens();
  if (validTokens.length === 0) {
    return false;
  }

  const authHeader = req.headers["authorization"] || "";
  let providedToken = "";

  if (authHeader.startsWith("Bearer ")) {
    providedToken = authHeader.substring(7).trim();
  } else if (req.headers["x-admin-token"]) {
    providedToken = req.headers["x-admin-token"].trim();
  } else if (req.query && req.query.token) {
    providedToken = req.query.token.trim();
  }

  if (!providedToken) {
    return false;
  }

  return validTokens.includes(providedToken);
}

// Retrieve allowlist of Google account emails for /admin access
function getAllowedEmails() {
  const defaultList = ["sameedjaved86@gmail.com"];
  if (process.env.ADMIN_EMAILS) {
    process.env.ADMIN_EMAILS.split(",").forEach((email) => {
      const clean = email.trim().toLowerCase();
      if (clean && !defaultList.includes(clean)) {
        defaultList.push(clean);
      }
    });
  }
  return defaultList;
}

// Shared handler for MacroDroid relay ingest
async function handleIngestRequest(req, res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Admin-Token");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  if (req.method !== "POST") {
    res.status(405).json({
      error: "Method not allowed. Use POST to send timetable data."
    });
    return;
  }

  if (!verifyAdminToken(req)) {
    // Record unauthorized attempt in logs for security auditing
    try {
      await db.collection("ingestLogs").add({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        success: false,
        ip: req.ip || req.headers["x-forwarded-for"] || "unknown",
        reason: "Unauthorized. Missing or invalid ADMIN_TOKEN."
      });
    } catch (_) {}

    res.status(401).json({
      error: "Unauthorized. Valid ADMIN_TOKEN required."
    });
    return;
  }

  try {
    const rawBody = req.body;
    let rawText = "";
    let extractedUrl = null;
    let sheetId = null;
    let exportUrl = null;
    let versionLabel = null;
    let customTimetable = null;

    if (typeof rawBody === "string") {
      rawText = rawBody.trim();
    } else if (Buffer.isBuffer(rawBody)) {
      rawText = rawBody.toString("utf8").trim();
    } else if (typeof rawBody === "object" && rawBody !== null) {
      if (typeof rawBody.text === "string") rawText = rawBody.text;
      if (typeof rawBody.url === "string") extractedUrl = rawBody.url;
      if (typeof rawBody.version === "string") versionLabel = rawBody.version;
      if (rawBody.timetable) customTimetable = rawBody.timetable;
      if (!rawText && !extractedUrl && !customTimetable) {
        rawText = JSON.stringify(rawBody);
      }
    }

    // Extract Google Sheets / Google Drive URL if present in text
    if (!extractedUrl && rawText) {
      const urlMatch = rawText.match(/https?:\/\/(?:docs|drive)\.google\.com\/[^\s"')]+/i);
      if (urlMatch) {
        extractedUrl = urlMatch[0];
      }
    }

    // Extract sheet ID if it is a Google Sheets document
    if (extractedUrl) {
      const idMatch = extractedUrl.match(/\/d\/([a-zA-Z0-9-_]+)/);
      if (idMatch) {
        sheetId = idMatch[1];
        exportUrl = "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=csv";
      }
    }

    const timestamp = Date.now();
    const versionId = versionLabel || ("v_" + timestamp);

    const versionDoc = {
      version: versionId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAtMillis: timestamp,
      source: "MacroDroid Relay",
      sourceUrl: extractedUrl || null,
      sheetId: sheetId || null,
      exportUrl: exportUrl || null,
      rawNotificationText: rawText || null,
      timetable: customTimetable || null
    };

    // Store in historical versions collection
    await db.collection("timetableVersions").doc(versionId).set(versionDoc);

    // Update single latest document for client app consumption
    await db.collection("timetableVersions").doc("latest").set(versionDoc);

    // Save successful ingest log
    await db.collection("ingestLogs").add({
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      success: true,
      version: versionId,
      extractedUrl: extractedUrl,
      rawText: rawText,
      ip: req.ip || req.headers["x-forwarded-for"] || "unknown"
    });

    res.status(200).json({
      success: true,
      message: "Timetable link ingested and published successfully.",
      version: versionId,
      extractedUrl: extractedUrl,
      timestamp: timestamp
    });
  } catch (error) {
    console.error("Failed to process relay ingest:", error);

    try {
      await db.collection("ingestLogs").add({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        success: false,
        error: error.message
      });
    } catch (_) {}

    res.status(500).json({
      error: "Internal server error processing ingest payload.",
      details: error.message
    });
  }
}

/**
 * 1. MacroDroid Relay Ingest Endpoint: /ingest
 */
exports.ingest = functions.https.onRequest(async (req, res) => {
  await handleIngestRequest(req, res);
});

/**
 * 2. API Router: handles /api/admin/ingest and /api/latest
 */
exports.api = functions.https.onRequest(async (req, res) => {
  const path = req.path || "";
  if (path.endsWith("/admin/ingest") || path.endsWith("/ingest")) {
    await handleIngestRequest(req, res);
    return;
  }
  if (path.endsWith("/latest")) {
    await handleLatestRequest(req, res);
    return;
  }
  res.status(404).json({ error: "Endpoint not found: " + path });
});

// Shared handler for public latest timetable
async function handleLatestRequest(req, res) {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }

  try {
    const doc = await db.collection("timetableVersions").doc("latest").get();
    if (!doc.exists) {
      res.status(404).json({
        error: "No timetable versions available yet."
      });
      return;
    }
    res.status(200).json(doc.data());
  } catch (error) {
    console.error("Failed to fetch latest timetable:", error);
    res.status(500).json({
      error: "Failed to fetch timetable.",
      details: error.message
    });
  }
}

/**
 * 3. Public Latest Timetable Endpoint: /latest
 */
exports.latest = functions.https.onRequest(async (req, res) => {
  await handleLatestRequest(req, res);
});

/**
 * 4. Protected Admin Page: /admin
 * Protected with Google Sign-In via Firebase Auth.
 * Limited to allowed administrator Google accounts.
 */
exports.admin = functions.https.onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");

  // Check for Firebase Auth ID token in Authorization header
  const authHeader = req.headers["authorization"] || "";
  let idToken = null;
  if (authHeader.startsWith("Bearer ")) {
    idToken = authHeader.substring(7).trim();
  }

  // Handle authenticated actions from admin dashboard
  if (req.method === "POST" && idToken) {
    try {
      const decoded = await admin.auth().verifyIdToken(idToken);
      const userEmail = (decoded.email || "").toLowerCase();
      const allowed = getAllowedEmails();

      if (!allowed.includes(userEmail)) {
        res.status(403).json({
          error: "Forbidden. Your Google account (" + userEmail + ") is not authorized."
        });
        return;
      }

      const payload = req.body || {};
      const action = req.query.action || payload.action || "publish";

      if (action === "getLogs") {
        const snap = await db.collection("ingestLogs")
          .orderBy("timestamp", "desc")
          .limit(20)
          .get();
        const logs = snap.docs.map(d => ({ id: d.id, ...d.data() }));
        res.status(200).json({ logs: logs });
        return;
      }

      const timestamp = Date.now();
      const versionId = payload.version || ("manual_" + timestamp);

      const versionDoc = {
        version: versionId,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAtMillis: timestamp,
        source: "Admin Portal (" + userEmail + ")",
        sourceUrl: payload.url || null,
        timetable: payload.timetable || payload.data || null
      };

      await db.collection("timetableVersions").doc(versionId).set(versionDoc);
      await db.collection("timetableVersions").doc("latest").set(versionDoc);

      res.status(200).json({
        success: true,
        message: "Manual timetable update published successfully.",
        version: versionId
      });
      return;
    } catch (e) {
      res.status(401).json({ error: "Invalid authorization token: " + e.message });
      return;
    }
  }

  // Serve the HTML page with Google Sign-In
  const allowedEmailsJson = JSON.stringify(getAllowedEmails());
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Class Flow Admin Portal - Thal University Bhakkar</title>
  <script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/10.8.0/firebase-auth-compat.js"></script>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
    .card { background: #1e293b; border-radius: 12px; padding: 24px; max-width: 760px; margin: 30px auto; border: 1px solid #334155; }
    h1 { margin-top: 0; color: #38bdf8; font-size: 24px; }
    button { background: #0284c7; color: white; border: none; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; }
    button:hover { background: #0369a1; }
    pre { background: #0f172a; padding: 12px; border-radius: 8px; overflow-x: auto; color: #a5f3fc; font-size: 13px; }
    textarea, input[type="text"] { width: 100%; background: #0f172a; color: #f8fafc; border: 1px solid #334155; border-radius: 8px; padding: 10px; font-family: monospace; box-sizing: border-box; }
    .status { padding: 12px; border-radius: 8px; margin-bottom: 16px; display: none; }
    .error { background: #7f1d1d; color: #fecaca; }
    .success { background: #14532d; color: #bbf7d0; }
    .log-item { background: #0f172a; border: 1px solid #334155; padding: 10px; border-radius: 6px; margin-bottom: 8px; font-size: 13px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Class Flow Admin Portal</h1>
    <p style="color: #94a3b8;">Thal University Bhakkar Timetable Management</p>

    <div id="status" class="status"></div>

    <div id="authSection">
      <p>This portal is restricted to authorized university administrators.</p>
      <button id="googleBtn" onclick="signInWithGoogle()">Sign In with Google</button>
    </div>

    <div id="adminContent" style="display: none;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <span id="userInfo" style="color: #94a3b8; font-weight: bold;"></span>
        <button style="background: #ef4444;" onclick="signOutUser()">Sign Out</button>
      </div>

      <h3>Current Latest Version in Firestore</h3>
      <div id="latestDisplay"><pre>Loading latest version...</pre></div>

      <h3>Manual Update: Paste Google Sheet Link</h3>
      <input type="text" id="manualUrl" placeholder="https://docs.google.com/spreadsheets/d/YOUR_SHEET_ID/edit?usp=sharing">
      <br><br>
      <button onclick="publishUrl()">Publish Link</button>

      <h3 style="margin-top: 30px;">Recent Relay Ingest Attempts (MacroDroid Logs)</h3>
      <div id="logsDisplay"><pre>Loading logs...</pre></div>

      <h3 style="margin-top: 30px;">MacroDroid Ingest Configuration</h3>
      <p style="font-size: 14px; color: #94a3b8;">
        MacroDroid forwards notifications directly using your ADMIN_TOKEN without requiring Google Sign-In.
      </p>
      <pre>POST https://${req.headers.host}/api/admin/ingest
Headers:
  Authorization: Bearer [ADMIN_TOKEN]
  Content-Type: text/plain
Body:
  [Notification Text containing Google Sheet link]</pre>
    </div>
  </div>

  <script>
    const allowedEmails = ${allowedEmailsJson};
    let currentIdToken = null;

    const firebaseConfig = {
      projectId: "${process.env.GCLOUD_PROJECT || "thal-university-timetable"}"
    };
    if (!firebase.apps.length) {
      firebase.initializeApp(firebaseConfig);
    }

    const auth = firebase.auth();
    auth.onAuthStateChanged(async (user) => {
      const statusDiv = document.getElementById("status");
      if (user) {
        const email = (user.email || "").toLowerCase();
        if (allowedEmails.includes(email)) {
          document.getElementById("authSection").style.display = "none";
          document.getElementById("adminContent").style.display = "block";
          document.getElementById("userInfo").innerText = "Signed in as: " + email;
          currentIdToken = await user.getIdToken();
          fetchLatest();
          fetchLogs();
        } else {
          statusDiv.className = "status error";
          statusDiv.style.display = "block";
          statusDiv.innerText = "Access denied: Account " + email + " is not in the administrator allowlist.";
          auth.signOut();
        }
      } else {
        document.getElementById("authSection").style.display = "block";
        document.getElementById("adminContent").style.display = "none";
      }
    });

    function signInWithGoogle() {
      const provider = new firebase.auth.GoogleAuthProvider();
      auth.signInWithPopup(provider).catch(err => {
        const statusDiv = document.getElementById("status");
        statusDiv.className = "status error";
        statusDiv.style.display = "block";
        statusDiv.innerText = err.message;
      });
    }

    function signOutUser() {
      auth.signOut();
    }

    async function fetchLatest() {
      try {
        const res = await fetch("/latest");
        const data = await res.json();
        document.getElementById("latestDisplay").innerHTML = "<pre>" + JSON.stringify(data, null, 2) + "</pre>";
      } catch (e) {
        document.getElementById("latestDisplay").innerHTML = "<pre>Error loading latest version: " + e.message + "</pre>";
      }
    }

    async function fetchLogs() {
      try {
        const res = await fetch("/admin?action=getLogs", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + currentIdToken
          },
          body: JSON.stringify({ action: "getLogs" })
        });
        const data = await res.json();
        if (data.logs && data.logs.length) {
          let html = "";
          data.logs.forEach(log => {
            const statusColor = log.success ? "#22c55e" : "#ef4444";
            const statusText = log.success ? "SUCCESS" : "FAILED";
            html += '<div class="log-item">' +
              '<span style="color:' + statusColor + '; font-weight:bold;">[' + statusText + ']</span> ' +
              '<strong>' + (log.version || "Attempt") + '</strong>: ' +
              (log.extractedUrl || log.rawText || log.reason || log.error || "") +
              '</div>';
          });
          document.getElementById("logsDisplay").innerHTML = html;
        } else {
          document.getElementById("logsDisplay").innerHTML = "<pre>No ingest attempts recorded yet.</pre>";
        }
      } catch (e) {
        document.getElementById("logsDisplay").innerHTML = "<pre>Error fetching logs: " + e.message + "</pre>";
      }
    }

    async function publishUrl() {
      const url = document.getElementById("manualUrl").value.trim();
      const statusDiv = document.getElementById("status");
      if (!url) {
        statusDiv.className = "status error";
        statusDiv.style.display = "block";
        statusDiv.innerText = "Please enter a valid Google Sheets URL.";
        return;
      }

      try {
        const res = await fetch("/admin", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + currentIdToken
          },
          body: JSON.stringify({ url: url })
        });
        const out = await res.json();
        statusDiv.className = "status success";
        statusDiv.style.display = "block";
        statusDiv.innerText = out.message || "Published successfully!";
        fetchLatest();
        fetchLogs();
      } catch (e) {
        statusDiv.className = "status error";
        statusDiv.style.display = "block";
        statusDiv.innerText = "Error: " + e.message;
      }
    }
  </script>
</body>
</html>`;

  res.status(200).send(html);
});
