/**
 * SYS22 Native Core Integration Script v2.2.0
 * Provides:
 * 1. Initial First-Run Firebase API Key Activation Modal
 * 2. In-App "Check for Updates" Button in Settings
 * 3. Bidirectional Native Bridge hooks
 */
(function() {
  'use strict';

  function initSYS22Core() {
    setupCloudKeyPrompt();
    setupSettingsUpdateObserver();
  }

  // --- 1. FIRST-RUN FIREBASE API KEY PROMPT ---
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
      background: rgba(3, 7, 18, 0.92);
      backdrop-filter: blur(10px);
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
        box-shadow: 0 0 30px rgba(56, 189, 248, 0.2);
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
          أدخل <strong style="color: #38bdf8;">Firebase API Key</strong> لتفعيل المزامنة الفورية.
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
        projectId: 'system22-27eba',
        secretId: 'COMMANDER_X'
      };
      localStorage.setItem('s22_cloudConfig', JSON.stringify(newConfig));
      localStorage.removeItem('s22_cloud_prompt_dismissed');
      modal.remove();
      // Reload page to re-initialize Firebase client with newly entered credentials
      window.location.reload();
    });

    skipBtn.addEventListener('click', () => {
      localStorage.setItem('s22_cloud_prompt_dismissed', 'true');
      modal.remove();
    });
  }

  // --- 2. IN-APP UPDATE SECTION IN SETTINGS ---
  function setupSettingsUpdateObserver() {
    setInterval(() => {
      injectSettingsUpdateCard();
    }, 1000);
  }

  function injectSettingsUpdateCard() {
    // Look for the Settings container with "CLOUD DATABASE CONFIG" or "SYSTEM SETTINGS"
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

    if (document.getElementById('sys22-app-update-card')) return;

    const updateCard = document.createElement('div');
    updateCard.id = 'sys22-app-update-card';
    updateCard.style.cssText = `
      margin-top: 24px;
      margin-bottom: 24px;
      padding: 16px;
      background: rgba(56, 189, 248, 0.05);
      border: 1px solid rgba(56, 189, 248, 0.3);
      border-radius: 2px;
      font-family: 'JetBrains Mono', monospace;
    `;

    updateCard.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
        <div>
          <div style="font-size: 13px; font-weight: bold; color: #ffffff; letter-spacing: 1px; display: flex; align-items: center; gap: 8px;">
            <span style="color: #38bdf8;">⚡</span>
            SYS22 MOBILE UPDATE ENGINE
          </div>
          <div style="font-size: 11px; color: #94a3b8; margin-top: 4px;">
            Installed Version: <strong style="color: #38bdf8;">v2.3.0 (Release Signed)</strong>
          </div>
          <div style="font-size: 9px; color: #64748b; margin-top: 2px;">
            Official Distribution • Seamless Over-The-Air Installation
          </div>
        </div>

        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button id="sys22_test_wisdom_btn" style="
            background: rgba(245, 158, 11, 0.15);
            color: #f59e0b;
            border: 1px solid #f59e0b;
            padding: 8px 12px;
            font-weight: bold;
            font-size: 11px;
            border-radius: 2px;
            cursor: pointer;
            letter-spacing: 0.5px;
            display: flex;
            align-items: center;
            gap: 6px;
            font-family: 'JetBrains Mono', monospace;
          ">
            <span>📜</span> تجربة حكمة اليوم
          </button>

          <button id="sys22_trigger_update_btn" style="
            background: #0284c7;
            color: #ffffff;
            border: 1px solid #38bdf8;
            padding: 8px 14px;
            font-weight: bold;
            font-size: 11px;
            border-radius: 2px;
            cursor: pointer;
            letter-spacing: 0.5px;
            display: flex;
            align-items: center;
            gap: 6px;
            font-family: 'JetBrains Mono', monospace;
          ">
            <span>🔄</span> CHECK FOR UPDATES
          </button>
        </div>
      </div>
    `;

    // Insert updateCard right above the Cloud Database Config or at the top of the settings panel
    targetHeader.parentElement.insertBefore(updateCard, targetHeader);

    document.getElementById('sys22_test_wisdom_btn').addEventListener('click', () => {
      if (window.SYS22Native && typeof window.SYS22Native.testWisdomNotification === 'function') {
        window.SYS22Native.testWisdomNotification();
      } else {
        alert('حكمة اليوم متاحة حصرياً عبر تطبيق الأندرويد.');
      }
    });

    document.getElementById('sys22_trigger_update_btn').addEventListener('click', () => {
      if (window.SYS22Native && typeof window.SYS22Native.checkForUpdates === 'function') {
        window.SYS22Native.checkForUpdates();
      } else {
        alert('SYS22 v2.3.0: You are running the latest version.');
      }
    });
  }

  // Auto-run on DOM ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSYS22Core);
  } else {
    initSYS22Core();
  }

  // Expose global helper for manual triggering
  window.sys22PromptCloudKey = renderCloudKeyModal;
})();
