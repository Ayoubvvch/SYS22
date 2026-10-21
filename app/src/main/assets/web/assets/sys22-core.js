/**
 * SYS22 Mobile Core & Cloud Synchronization Engine v2.4.0
 * Architecture:
 * 1. Initial First-Run Firebase API Key Activation Modal
 * 2. Instant Bidirectional Cloud Sync Engine (REST + Realtime Listeners)
 * 3. Smart Non-Destructive Merging for Tasks, Exams & Rules
 * 4. Zero-Lag Native Alarm & Notification Synchronization
 * 5. In-App Updates & Daily Tactical Wisdom Triggers
 * 6. Visual Cloud Status Badges & On-Demand Sync Controls
 */
(function() {
  'use strict';

  const APP_VERSION = 'v2.4.0';
  const DEFAULT_PROJECT_ID = 'system22-27eba';
  const DEFAULT_SECRET_ID = 'COMMANDER_X';
  const DEFAULT_API_KEY = 'AIzaSyA6tfbm27MexGAbxZUI6pIDxJu-z46u-Zs';

  let isSyncInProgress = false;
  let lastSyncTimestamp = null;
  let syncStatusText = 'READY';

  // --- 1. CONFIGURATION HELPER ---
  function getCloudConfig() {
    try {
      const cfgStr = localStorage.getItem('s22_cloudConfig');
      if (cfgStr) {
        const cfg = JSON.parse(cfgStr);
        return {
          apiKey: (cfg && cfg.apiKey && cfg.apiKey.trim()) || DEFAULT_API_KEY,
          projectId: (cfg && cfg.projectId && cfg.projectId.trim()) || DEFAULT_PROJECT_ID,
          secretId: (cfg && cfg.secretId && cfg.secretId.trim()) || DEFAULT_SECRET_ID
        };
      }
    } catch (_) {}
    return {
      apiKey: DEFAULT_API_KEY,
      projectId: DEFAULT_PROJECT_ID,
      secretId: DEFAULT_SECRET_ID
    };
  }

  // --- 2. INITIAL FIRST-RUN FIREBASE API KEY PROMPT ---
  function setupCloudKeyPrompt() {
    try {
      const configStr = localStorage.getItem('s22_cloudConfig');
      const dismissed = localStorage.getItem('s22_cloud_prompt_dismissed');
      let hasValidKey = false;
      if (configStr) {
        try {
          const cfg = JSON.parse(configStr);
          if (cfg && cfg.apiKey && cfg.apiKey.trim().length > 5) {
            hasValidKey = true;
          }
        } catch (_) {}
      }

      if (!hasValidKey && !dismissed) {
        setTimeout(renderCloudKeyModal, 700);
      }
    } catch (e) {
      console.error('[SYS22 Core] Error in setupCloudKeyPrompt:', e);
    }
  }

  function renderCloudKeyModal() {
    if (document.getElementById('sys22-cloud-key-modal')) return;

    const modal = document.createElement('div');
    modal.id = 'sys22-cloud-key-modal';
    modal.style.cssText = `
      position: fixed;
      top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(3, 7, 18, 0.94);
      backdrop-filter: blur(12px);
      z-index: 999999;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      font-family: 'JetBrains Mono', monospace;
      box-sizing: border-box;
      direction: rtl;
    `;

    modal.innerHTML = `
      <div style="
        background: #030712;
        border: 1px solid #38bdf8;
        box-shadow: 0 0 35px rgba(56, 189, 248, 0.25);
        border-radius: 4px;
        width: 100%;
        max-width: 480px;
        padding: 24px;
        color: #e2e8f0;
      ">
        <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px;">
          <span style="font-size: 24px; color: #38bdf8;">⚡</span>
          <h2 style="font-size: 16px; font-weight: bold; margin: 0; color: #ffffff; letter-spacing: 1px;">
            SYS22: إعداد المزامنة السحابية الأولية
          </h2>
        </div>

        <p style="font-size: 12px; color: #94a3b8; line-height: 1.6; margin-bottom: 16px;">
          للحفاظ على خصوصيتك وأمان بياناتك، تم جعل التطبيق آمناً ومفتوحاً للجميع.
          أدخل <strong style="color: #38bdf8;">Firebase API Key</strong> لتفعيل المزامنة الفورية مع الموقع.
          سيتم حفظ المفتاح محلياً على هذا الجهاز ولن تحتاج لإدخاله مجدداً!
        </p>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 11px; color: #cbd5e1; margin-bottom: 6px; font-weight: bold;">
            FIREBASE WEB API KEY:
          </label>
          <input id="sys22_modal_api_key" type="password" placeholder="AIzaSy..." style="
            width: 100%;
            background: rgba(0, 0, 0, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.2);
            color: #ffffff;
            padding: 10px 12px;
            font-size: 13px;
            font-family: 'JetBrains Mono', monospace;
            border-radius: 2px;
            outline: none;
            box-sizing: border-box;
            direction: ltr;
          " />
        </div>

        <div style="
          background: rgba(56, 189, 248, 0.05);
          border: 1px dashed rgba(56, 189, 248, 0.3);
          padding: 8px 12px;
          border-radius: 2px;
          margin-bottom: 20px;
          font-size: 10px;
          color: #94a3b8;
          direction: ltr;
        ">
          <div><strong style="color: #38bdf8;">PROJECT ID:</strong> system22-27eba (Default)</div>
          <div><strong style="color: #38bdf8;">SECRET ID:</strong> COMMANDER_X (Default)</div>
        </div>

        <div style="display: flex; gap: 10px; flex-direction: column;">
          <button id="sys22_modal_save_btn" style="
            width: 100%;
            background: #0284c7;
            color: #ffffff;
            border: 1px solid #38bdf8;
            padding: 12px;
            font-weight: bold;
            font-size: 13px;
            cursor: pointer;
            border-radius: 2px;
            font-family: 'JetBrains Mono', monospace;
            letter-spacing: 1px;
          ">
            💾 حفظ وتفعيل المزامنة (CONNECT)
          </button>

          <button id="sys22_modal_skip_btn" style="
            width: 100%;
            background: transparent;
            color: #64748b;
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 8px;
            font-size: 11px;
            cursor: pointer;
            border-radius: 2px;
            font-family: 'JetBrains Mono', monospace;
          ">
            المتابعة بدون مزامنة (Offline Mode)
          </button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    const input = document.getElementById('sys22_modal_api_key');
    const saveBtn = document.getElementById('sys22_modal_save_btn');
    const skipBtn = document.getElementById('sys22_modal_skip_btn');

    saveBtn.addEventListener('click', () => {
      const keyVal = (input.value || '').trim();
      if (!keyVal) {
        alert('يرجى إدخال مفتاح Firebase API Key للمتابعة');
        return;
      }
      const newConfig = {
        apiKey: keyVal,
        projectId: DEFAULT_PROJECT_ID,
        secretId: DEFAULT_SECRET_ID
      };
      localStorage.setItem('s22_cloudConfig', JSON.stringify(newConfig));
      localStorage.removeItem('s22_cloud_prompt_dismissed');
      modal.remove();
      window.location.reload();
    });

    skipBtn.addEventListener('click', () => {
      localStorage.setItem('s22_cloud_prompt_dismissed', 'true');
      modal.remove();
    });
  }

  // --- 3. FIRESTORE REST PROTOCOL ENGINE ---
  function parseFirestoreFields(fields) {
    if (!fields) return {};
    const parsed = {};
    for (const [key, valObj] of Object.entries(fields)) {
      if (!valObj) continue;
      if (valObj.stringValue !== undefined) {
        parsed[key] = valObj.stringValue;
      } else if (valObj.integerValue !== undefined) {
        parsed[key] = parseInt(valObj.integerValue, 10);
      } else if (valObj.doubleValue !== undefined) {
        parsed[key] = parseFloat(valObj.doubleValue);
      } else if (valObj.booleanValue !== undefined) {
        parsed[key] = valObj.booleanValue;
      }
    }
    return parsed;
  }

  function serializeToFirestoreFields(dataObj) {
    const fields = {};
    for (const [key, val] of Object.entries(dataObj)) {
      if (val === undefined || val === null) continue;
      if (typeof val === 'number') {
        if (Number.isInteger(val)) {
          fields[key] = { integerValue: val.toString() };
        } else {
          fields[key] = { doubleValue: val };
        }
      } else if (typeof val === 'boolean') {
        fields[key] = { booleanValue: val };
      } else {
        fields[key] = { stringValue: String(val) };
      }
    }
    return { fields };
  }

  // --- 4. SMART NON-DESTRUCTIVE MERGE ALGORITHM ---
  function mergeArrayById(localArr, cloudArr) {
    const map = new Map();
    // 1. Load cloud items first
    if (Array.isArray(cloudArr)) {
      for (const item of cloudArr) {
        if (item && item.id !== undefined) {
          map.set(String(item.id), item);
        }
      }
    }
    // 2. Merge local items
    if (Array.isArray(localArr)) {
      for (const item of localArr) {
        if (!item || item.id === undefined) continue;
        const idStr = String(item.id);
        if (!map.has(idStr)) {
          map.set(idStr, item);
        } else {
          const cloudItem = map.get(idStr);
          // Preserve completed state or newer updates
          map.set(idStr, { ...cloudItem, ...item });
        }
      }
    }
    return Array.from(map.values());
  }

  function safeJsonParse(str, fallback) {
    if (!str) return fallback;
    try {
      return JSON.parse(str);
    } catch (_) {
      return fallback;
    }
  }

  // --- 5. NATIVE ALARM DISPATCH HELPER ---
  function syncNativeAndroidEngine() {
    try {
      if (window.SYS22Native && typeof window.SYS22Native.syncAll === 'function') {
        const payload = {
          criticalTargets: localStorage.getItem('s22_criticalTargets') || '[]',
          tasks: localStorage.getItem('s22_tasks') || '[]',
          rules: localStorage.getItem('s22_rules') || '[]',
          morningRules: localStorage.getItem('s22_morningRules') || '[]',
          patterns: localStorage.getItem('s22_patterns') || '[]',
          wakeTime: localStorage.getItem('s22_wakeTime') || '03:30'
        };
        window.SYS22Native.syncAll(JSON.stringify(payload));
      }
    } catch (e) {
      console.error('[SYS22 Core] Error syncing with native engine:', e);
    }
  }

  // --- 6. TOAST NOTIFICATION HELPER ---
  function showTacticalToast(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.style.cssText = `
      position: fixed;
      bottom: 24px;
      left: 50%;
      transform: translateX(-50%);
      background: ${type === 'success' ? '#064e3b' : type === 'error' ? '#7f1d1d' : '#0f172a'};
      color: #ffffff;
      border: 1px solid ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#38bdf8'};
      padding: 10px 18px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: bold;
      letter-spacing: 0.5px;
      z-index: 1000000;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6);
      font-family: 'JetBrains Mono', monospace;
      direction: rtl;
      text-align: center;
      display: flex;
      align-items: center;
      gap: 8px;
      pointer-events: none;
      transition: opacity 0.3s ease;
    `;
    toast.innerHTML = `<span>${type === 'success' ? '✅' : type === 'error' ? '⚠️' : '⚡'}</span> <span>${msg}</span>`;
    document.body.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 400);
    }, 3200);
  }

  // --- 7. CORE CLOUD SYNCHRONIZATION CONTROLLER ---
  const SYS22Sync = {
    // A. Pull latest data from Firestore REST
    pullFromCloud: async function(force = false) {
      if (isSyncInProgress) return false;
      isSyncInProgress = true;
      updateSyncUIStatus('SYNCING');

      const config = getCloudConfig();
      const url = `https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/systems/${config.secretId}?key=${config.apiKey}`;

      try {
        const resp = await fetch(url, { method: 'GET', headers: { 'Accept': 'application/json' } });
        if (!resp.ok) {
          throw new Error(`Cloud fetch failed with status: ${resp.status}`);
        }

        const data = await resp.json();
        const fields = parseFirestoreFields(data.fields);

        const cloudTime = fields.lastUpdate ? new Date(fields.lastUpdate).getTime() : 0;
        const localTimeStr = localStorage.getItem('s22_lastUpdate');
        const localTime = localTimeStr ? new Date(localTimeStr).getTime() : 0;

        // If cloud is newer or force sync requested
        if (cloudTime > localTime || force) {
          // Parse Cloud Data
          const cloudTasks = safeJsonParse(fields.tasks, []);
          const cloudTargets = safeJsonParse(fields.criticalTargets, []);
          const cloudPatterns = safeJsonParse(fields.patterns, []);
          const cloudRules = safeJsonParse(fields.rules, []);
          const cloudMorningRules = safeJsonParse(fields.morningRules, []);
          const cloudArchive = safeJsonParse(fields.archive, []);
          const cloudQuickSubjects = safeJsonParse(fields.quickSubjects, null);
          const cloudDayExcluded = safeJsonParse(fields.dayExcludedBlocks, {});
          const cloudCheckIn = safeJsonParse(fields.checkInHistory, {});
          const cloudProtocolLogs = safeJsonParse(fields.protocolLogs, {});

          // Parse Local Data
          const localTasks = safeJsonParse(localStorage.getItem('s22_tasks'), []);
          const localTargets = safeJsonParse(localStorage.getItem('s22_criticalTargets'), []);
          const localPatterns = safeJsonParse(localStorage.getItem('s22_patterns'), []);
          const localRules = safeJsonParse(localStorage.getItem('s22_rules'), []);
          const localMorningRules = safeJsonParse(localStorage.getItem('s22_morningRules'), []);

          // Non-Destructive Merges
          const mergedTasks = mergeArrayById(localTasks, cloudTasks);
          const mergedTargets = mergeArrayById(localTargets, cloudTargets);
          const mergedPatterns = mergeArrayById(localPatterns, cloudPatterns);
          const mergedRules = mergeArrayById(localRules, cloudRules);
          const mergedMorningRules = cloudMorningRules.length > 0 ? cloudMorningRules : localMorningRules;

          // Apply to Local Storage
          localStorage.setItem('s22_tasks', JSON.stringify(mergedTasks));
          localStorage.setItem('s22_criticalTargets', JSON.stringify(mergedTargets));
          localStorage.setItem('s22_patterns', JSON.stringify(mergedPatterns));
          localStorage.setItem('s22_rules', JSON.stringify(mergedRules));
          localStorage.setItem('s22_morningRules', JSON.stringify(mergedMorningRules));

          if (cloudArchive.length > 0) localStorage.setItem('s22_archive', JSON.stringify(cloudArchive));
          if (cloudQuickSubjects) localStorage.setItem('s22_quickSubjects', JSON.stringify(cloudQuickSubjects));
          if (Object.keys(cloudDayExcluded).length > 0) localStorage.setItem('s22_dayExcludedBlocks', JSON.stringify(cloudDayExcluded));
          if (Object.keys(cloudCheckIn).length > 0) localStorage.setItem('s22_checkInHistory', JSON.stringify(cloudCheckIn));
          if (Object.keys(cloudProtocolLogs).length > 0) localStorage.setItem('s22_protocolLogs', JSON.stringify(cloudProtocolLogs));

          if (fields.totalFocusMinutes !== undefined) localStorage.setItem('s22_totalFocus', String(fields.totalFocusMinutes));
          if (fields.startDate) localStorage.setItem('s22_startDate', fields.startDate);
          if (fields.targetDate) localStorage.setItem('s22_targetDate', fields.targetDate);
          if (fields.targetTime) localStorage.setItem('s22_targetTime', fields.targetTime);
          if (fields.wakeTime) localStorage.setItem('s22_wakeTime', fields.wakeTime);
          if (fields.sleepTime) localStorage.setItem('s22_sleepTime', fields.sleepTime);

          const newTimestamp = fields.lastUpdate || new Date().toISOString();
          localStorage.setItem('s22_lastUpdate', newTimestamp);
          lastSyncTimestamp = new Date();

          // Instantly sync alarms and notifications
          syncNativeAndroidEngine();

          updateSyncUIStatus('CONNECTED');
          return true;
        }

        lastSyncTimestamp = new Date();
        updateSyncUIStatus('CONNECTED');
        return false;
      } catch (err) {
        console.error('[SYS22 Sync] Pull error:', err);
        updateSyncUIStatus('ERROR');
        return false;
      } finally {
        isSyncInProgress = false;
      }
    },

    // B. Push local state to Firestore REST
    pushToCloud: async function() {
      if (isSyncInProgress) return false;
      isSyncInProgress = true;
      updateSyncUIStatus('SYNCING');

      const config = getCloudConfig();
      const url = `https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/systems/${config.secretId}?key=${config.apiKey}`;

      try {
        const nowIso = new Date().toISOString();
        const payloadData = {
          tasks: localStorage.getItem('s22_tasks') || '[]',
          criticalTargets: localStorage.getItem('s22_criticalTargets') || '[]',
          patterns: localStorage.getItem('s22_patterns') || '[]',
          rules: localStorage.getItem('s22_rules') || '[]',
          morningRules: localStorage.getItem('s22_morningRules') || '[]',
          archive: localStorage.getItem('s22_archive') || '[]',
          quickSubjects: localStorage.getItem('s22_quickSubjects') || '[]',
          dayExcludedBlocks: localStorage.getItem('s22_dayExcludedBlocks') || '{}',
          checkInHistory: localStorage.getItem('s22_checkInHistory') || '{}',
          protocolLogs: localStorage.getItem('s22_protocolLogs') || '{}',
          totalFocusMinutes: parseInt(localStorage.getItem('s22_totalFocus') || '0', 10),
          startDate: localStorage.getItem('s22_startDate') || '',
          targetDate: localStorage.getItem('s22_targetDate') || '',
          targetTime: localStorage.getItem('s22_targetTime') || '00:00',
          wakeTime: localStorage.getItem('s22_wakeTime') || '03:30',
          sleepTime: localStorage.getItem('s22_sleepTime') || '20:30',
          lastUpdate: nowIso,
          clientOrigin: 'sys22_mobile_' + Math.random().toString(36).substring(2, 8)
        };

        const firestoreBody = serializeToFirestoreFields(payloadData);

        const resp = await fetch(url, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(firestoreBody)
        });

        if (!resp.ok) {
          throw new Error(`Cloud push failed with status: ${resp.status}`);
        }

        localStorage.setItem('s22_lastUpdate', nowIso);
        lastSyncTimestamp = new Date();
        syncNativeAndroidEngine();
        updateSyncUIStatus('CONNECTED');
        return true;
      } catch (err) {
        console.error('[SYS22 Sync] Push error:', err);
        updateSyncUIStatus('ERROR');
        return false;
      } finally {
        isSyncInProgress = false;
      }
    },

    // C. Full Bi-directional Master Sync (Pull + Merge + Push + UI Refresh)
    fullSync: async function(showFeedback = true) {
      if (showFeedback) {
        showTacticalToast('جاري المزامنة مع خادم الموقع والسحابة...', 'info');
      }

      const hadNewCloudData = await SYS22Sync.pullFromCloud(true);
      await SYS22Sync.pushToCloud();

      syncNativeAndroidEngine();

      if (showFeedback) {
        showTacticalToast('✅ تم التزامن التام مع الموقع (تم تحديث المهام والإشعارات)', 'success');
        // Gentle reload to refresh React UI with freshly merged state
        setTimeout(() => {
          window.location.reload();
        }, 500);
      }
    }
  };

  // --- 8. REAL-TIME STORAGE HOOK FOR INSTANT ALARM SYNC ---
  function hookLocalStorage() {
    try {
      const originalSetItem = localStorage.setItem;
      localStorage.setItem = function(key, val) {
        originalSetItem.apply(this, arguments);

        // When cloud snapshot or React updates s22_lastUpdate, immediately sync alarms
        if (key === 's22_lastUpdate') {
          syncNativeAndroidEngine();
          updateSyncUIStatus('CONNECTED');
        }
      };
    } catch (e) {
      console.error('[SYS22 Core] Error hooking localStorage:', e);
    }
  }

  // --- 9. LIFECYCLE SYNC HOOKS (Triggered by Android Native) ---
  window.sys22OnResume = async function() {
    console.log('[SYS22 Core] App resumed from background. Checking cloud sync parity...');
    try {
      const updated = await SYS22Sync.pullFromCloud(false);
      if (updated) {
        showTacticalToast('⚡ تم تحديث البيانات تلقائياً من الموقع', 'info');
        setTimeout(() => window.location.reload(), 400);
      }
    } catch (e) {
      console.warn('[SYS22 Core] Resume sync check failed:', e);
    }
  };

  window.sys22ForceCloudSync = function() {
    SYS22Sync.fullSync(true);
  };

  // --- 10. UI INJECTIONS: SYNC BADGE & SETTINGS DASHBOARD ---
  function updateSyncUIStatus(status) {
    syncStatusText = status;
    const badge = document.getElementById('sys22-sync-badge');
    if (badge) {
      if (status === 'SYNCING') {
        badge.innerHTML = `<span style="color: #38bdf8;">🔄</span> <span style="color: #38bdf8;">SYNCING</span>`;
      } else if (status === 'CONNECTED') {
        const timeStr = lastSyncTimestamp ? lastSyncTimestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
        badge.innerHTML = `<span style="color: #22c55e;">🟢</span> <span style="color: #22c55e;">SYNCED ${timeStr}</span>`;
      } else if (status === 'ERROR') {
        badge.innerHTML = `<span style="color: #ef4444;">⚠️</span> <span style="color: #ef4444;">SYNC ERROR</span>`;
      } else {
        badge.innerHTML = `<span style="color: #94a3b8;">⚡</span> <span>READY</span>`;
      }
    }

    const panelStatus = document.getElementById('sys22-panel-sync-status');
    if (panelStatus) {
      if (status === 'SYNCING') {
        panelStatus.innerText = 'جاري المزامنة مع الموقع...';
        panelStatus.style.color = '#38bdf8';
      } else if (status === 'CONNECTED') {
        panelStatus.innerText = 'متزامن مع الموقع والسحابة بنجاح';
        panelStatus.style.color = '#22c55e';
      } else if (status === 'ERROR') {
        panelStatus.innerText = 'خطأ في الاتصال بالخادم';
        panelStatus.style.color = '#ef4444';
      }
    }
  }

  function injectHeaderSyncBadge() {
    // Look for the mobile header with class 'md:hidden'
    const headers = document.querySelectorAll('.md\\:hidden');
    let mobileHeader = null;
    for (let i = 0; i < headers.length; i++) {
      if (headers[i].textContent.includes('SYS22')) {
        mobileHeader = headers[i];
        break;
      }
    }
    if (!mobileHeader) return;
    if (document.getElementById('sys22-sync-badge')) return;

    const badge = document.createElement('div');
    badge.id = 'sys22-sync-badge';
    badge.style.cssText = `
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 3px 8px;
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid rgba(56, 189, 248, 0.3);
      border-radius: 12px;
      font-size: 9px;
      font-weight: bold;
      letter-spacing: 0.5px;
      color: #94a3b8;
      cursor: pointer;
      font-family: 'JetBrains Mono', monospace;
      margin-left: 6px;
    `;
    badge.innerHTML = `<span style="color: #22c55e;">🟢</span> <span>SYNC</span>`;
    badge.title = 'انقر للمزامنة الفورية مع الموقع';

    badge.addEventListener('click', () => {
      SYS22Sync.fullSync(true);
    });

    const rightContainer = mobileHeader.querySelector('.flex.items-center.gap-3');
    if (rightContainer) {
      rightContainer.insertBefore(badge, rightContainer.firstChild);
    }
  }

  function injectSettingsCards() {
    const headers = document.querySelectorAll('h3, h2');
    let targetHeader = null;
    for (let i = 0; i < headers.length; i++) {
      if (headers[i].textContent.includes('CLOUD DATABASE CONFIG')) {
        targetHeader = headers[i];
        break;
      }
    }

    if (!targetHeader) return;
    const parentContainer = targetHeader.closest('.glass-panel') || targetHeader.parentElement;
    if (!parentContainer) return;

    if (document.getElementById('sys22-unified-sync-card')) return;

    const card = document.createElement('div');
    card.id = 'sys22-unified-sync-card';
    card.style.cssText = `
      margin-top: 20px;
      margin-bottom: 24px;
      padding: 16px;
      background: rgba(15, 23, 42, 0.7);
      border: 1px solid #38bdf8;
      box-shadow: 0 0 20px rgba(56, 189, 248, 0.15);
      border-radius: 4px;
      font-family: 'JetBrains Mono', monospace;
    `;

    card.innerHTML = `
      <!-- Header -->
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; border-b: 1px solid rgba(255, 255, 255, 0.1); padding-bottom: 10px;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 18px; color: #38bdf8;">⚡</span>
          <div>
            <div style="font-size: 13px; font-weight: bold; color: #ffffff; letter-spacing: 1px;">
              SYS22 CLOUD SYNCHRONIZER
            </div>
            <div style="font-size: 10px; color: #94a3b8;">
              تزامن ثنائي الاتجاه فوري مع نسخة الموقع الأصلية (${APP_VERSION})
            </div>
          </div>
        </div>

        <div id="sys22-panel-sync-status" style="font-size: 11px; font-weight: bold; color: #22c55e;">
          متصل ومتزامن
        </div>
      </div>

      <!-- Sync Actions Grid -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 14px;">
        <button id="sys22_btn_force_sync" style="
          grid-column: span 2;
          background: #0284c7;
          color: #ffffff;
          border: 1px solid #38bdf8;
          padding: 11px;
          font-weight: bold;
          font-size: 12px;
          border-radius: 2px;
          cursor: pointer;
          letter-spacing: 0.8px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          font-family: 'JetBrains Mono', monospace;
        ">
          <span>⚡</span> مزامنة فورية كاملة (FORCE MASTER SYNC)
        </button>

        <button id="sys22_btn_pull_cloud" style="
          background: rgba(56, 189, 248, 0.1);
          color: #38bdf8;
          border: 1px solid rgba(56, 189, 248, 0.4);
          padding: 8px;
          font-size: 11px;
          font-weight: bold;
          border-radius: 2px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          font-family: 'JetBrains Mono', monospace;
        ">
          <span>📥</span> سحب من الموقع (Pull)
        </button>

        <button id="sys22_btn_push_cloud" style="
          background: rgba(16, 185, 129, 0.1);
          color: #10b981;
          border: 1px solid rgba(16, 185, 129, 0.4);
          padding: 8px;
          font-size: 11px;
          font-weight: bold;
          border-radius: 2px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          font-family: 'JetBrains Mono', monospace;
        ">
          <span>📤</span> رفع إلى الموقع (Push)
        </button>
      </div>

      <!-- App Tools (Wisdom + Updater) -->
      <div style="display: flex; gap: 8px; flex-wrap: wrap; border-top: 1px solid rgba(255, 255, 255, 0.08); padding-top: 12px;">
        <button id="sys22_btn_wisdom" style="
          flex: 1;
          min-width: 140px;
          background: rgba(245, 158, 11, 0.12);
          color: #f59e0b;
          border: 1px solid #f59e0b;
          padding: 8px 10px;
          font-weight: bold;
          font-size: 11px;
          border-radius: 2px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          font-family: 'JetBrains Mono', monospace;
        ">
          <span>📜</span> تجربة حكمة اليوم
        </button>

        <button id="sys22_btn_update" style="
          flex: 1;
          min-width: 140px;
          background: rgba(14, 165, 233, 0.15);
          color: #38bdf8;
          border: 1px solid #38bdf8;
          padding: 8px 10px;
          font-weight: bold;
          font-size: 11px;
          border-radius: 2px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          font-family: 'JetBrains Mono', monospace;
        ">
          <span>🔄</span> CHECK FOR UPDATES
        </button>
      </div>
    `;

    targetHeader.parentElement.insertBefore(card, targetHeader);

    // Event Listeners
    document.getElementById('sys22_btn_force_sync').addEventListener('click', () => {
      SYS22Sync.fullSync(true);
    });

    document.getElementById('sys22_btn_pull_cloud').addEventListener('click', async () => {
      showTacticalToast('جاري سحب البيانات من الموقع...', 'info');
      const updated = await SYS22Sync.pullFromCloud(true);
      if (updated) {
        showTacticalToast('✅ تم جلب ودمج البيانات من الموقع بنجاح', 'success');
        setTimeout(() => window.location.reload(), 400);
      } else {
        showTacticalToast('النظام متزامن بالفعل مع الموقع', 'info');
      }
    });

    document.getElementById('sys22_btn_push_cloud').addEventListener('click', async () => {
      showTacticalToast('جاري رفع البيانات إلى الموقع...', 'info');
      const success = await SYS22Sync.pushToCloud();
      if (success) {
        showTacticalToast('✅ تم رفع بيانات الهاتف إلى الموقع بنجاح', 'success');
      } else {
        showTacticalToast('فشل الرفع إلى الموقع، تحقق من الإنترنت', 'error');
      }
    });

    document.getElementById('sys22_btn_wisdom').addEventListener('click', () => {
      if (window.SYS22Native && typeof window.SYS22Native.testWisdomNotification === 'function') {
        window.SYS22Native.testWisdomNotification();
      } else {
        alert('حكمة اليوم متاحة حصرياً عبر تطبيق الأندرويد.');
      }
    });

    document.getElementById('sys22_btn_update').addEventListener('click', () => {
      if (window.SYS22Native && typeof window.SYS22Native.checkForUpdates === 'function') {
        window.SYS22Native.checkForUpdates();
      } else {
        alert(`SYS22 ${APP_VERSION}: You are running the latest version.`);
      }
    });
  }

  // --- 11. INITIALIZATION ---
  function initSYS22Core() {
    hookLocalStorage();
    setupCloudKeyPrompt();

    // Regular UI checks
    setInterval(() => {
      injectSettingsCards();
      injectHeaderSyncBadge();
    }, 1000);

    // Initial background silent pull to check if web made updates
    setTimeout(() => {
      SYS22Sync.pullFromCloud(false);
    }, 2000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSYS22Core);
  } else {
    initSYS22Core();
  }

  // Global helper exports
  window.sys22PromptCloudKey = renderCloudKeyModal;
  window.sys22SyncEngine = SYS22Sync;
})();
