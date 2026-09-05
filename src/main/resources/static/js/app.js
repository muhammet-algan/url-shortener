/**
 * URL Shortener — Frontend Application
 *
 * Handles URL shortening, analytics display, recent links,
 * and all UI interactions. Communicates with the Spring Boot backend API.
 */

const API_BASE = '/api/v1';

// ── DOM Elements ──
const urlInput = document.getElementById('urlInput');
const shortenBtn = document.getElementById('shortenBtn');
const optionsToggle = document.getElementById('optionsToggle');
const optionsPanel = document.getElementById('optionsPanel');
const customCodeInput = document.getElementById('customCode');
const ttlSelect = document.getElementById('ttlSelect');
const resultArea = document.getElementById('resultArea');
const resultUrl = document.getElementById('resultUrl');
const resultExpiry = document.getElementById('resultExpiry');
const resultCode = document.getElementById('resultCode');
const copyBtn = document.getElementById('copyBtn');
const qrBtn = document.getElementById('qrBtn');
const qrModal = document.getElementById('qrModal');
const closeQrModal = document.getElementById('closeQrModal');
const qrImage = document.getElementById('qrImage');
const downloadQrBtn = document.getElementById('downloadQrBtn');
let currentShortCode = '';
const errorArea = document.getElementById('errorArea');
const errorMessage = document.getElementById('errorMessage');
const analyticsInput = document.getElementById('analyticsInput');
const analyticsBtn = document.getElementById('analyticsBtn');
const analyticsResult = document.getElementById('analyticsResult');
const recentList = document.getElementById('recentList');
const toast = document.getElementById('toast');
const toastMessage = document.getElementById('toastMessage');

// ── Local Storage ──
const STORAGE_KEY = 'recent_urls';

function getRecentUrls() {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
    } catch { return []; }
}

function saveRecentUrl(data) {
    const recent = getRecentUrls();
    recent.unshift({
        shortCode: data.shortCode,
        shortUrl: data.shortUrl,
        originalUrl: data.originalUrl,
        createdAt: data.createdAt,
        expiresAt: data.expiresAt
    });
    // Keep last 20
    localStorage.setItem(STORAGE_KEY, JSON.stringify(recent.slice(0, 20)));
    updateStats();
}

// ════════════════════════════════════════════════════════
//  TAB NAVIGATION
// ════════════════════════════════════════════════════════

document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        // Update nav buttons
        document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
        const tabId = 'tab-' + btn.dataset.tab;
        document.getElementById(tabId).classList.add('active');

        // Render recent list when switching to that tab
        if (btn.dataset.tab === 'recent') {
            renderRecentList();
        }
    });
});

// ════════════════════════════════════════════════════════
//  SHORTEN URL
// ════════════════════════════════════════════════════════

shortenBtn.addEventListener('click', shortenUrl);
urlInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') shortenUrl();
});

async function shortenUrl() {
    const url = urlInput.value.trim();
    if (!url) {
        showError('Lütfen bir URL girin.');
        urlInput.focus();
        return;
    }

    // Basic URL validation
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
        showError('URL "http://" veya "https://" ile başlamalıdır.');
        return;
    }

    hideError();
    hideResult();
    setLoading(true);

    const body = {
        originalUrl: url,
        ttlHours: parseInt(ttlSelect.value)
    };

    const customCode = customCodeInput.value.trim();
    if (customCode) {
        body.customCode = customCode;
    }

    try {
        const response = await fetch(`${API_BASE}/urls`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            const error = await response.json();
            if (response.status === 429) {
                showError(`Çok fazla istek gönderdiniz. ${error.retryAfterSeconds || 60} saniye sonra tekrar deneyin.`);
            } else if (response.status === 409) {
                showError(`"${customCode}" kodu zaten kullanımda. Başka bir kod deneyin.`);
            } else {
                showError(error.message || 'Bir hata oluştu.');
            }
            return;
        }

        const data = await response.json();
        showResult(data);
        saveRecentUrl(data);

        // Clear inputs
        urlInput.value = '';
        customCodeInput.value = '';

        showToast('Link başarıyla kısaltıldı! 🎉');
    } catch (err) {
        showError('Sunucuya bağlanılamadı. Lütfen tekrar deneyin.');
        console.error('Shorten error:', err);
    } finally {
        setLoading(false);
    }
}

function showResult(data) {
    currentShortCode = data.shortCode;
    const fullUrl = window.location.origin + '/' + data.shortCode;
    resultUrl.textContent = fullUrl;
    resultUrl.href = fullUrl;
    resultCode.textContent = data.shortCode;
    resultExpiry.textContent = formatDate(data.expiresAt);
    resultArea.hidden = false;
}

function hideResult() {
    resultArea.hidden = true;
}

function showError(msg) {
    errorMessage.textContent = msg;
    errorArea.hidden = false;
}

function hideError() {
    errorArea.hidden = true;
}

function setLoading(loading) {
    const btnText = shortenBtn.querySelector('.btn-text');
    const btnLoader = shortenBtn.querySelector('.btn-loader');
    btnText.hidden = loading;
    btnLoader.hidden = !loading;
    shortenBtn.disabled = loading;
}

// ── Copy to Clipboard ──
copyBtn.addEventListener('click', async () => {
    const url = resultUrl.textContent;
    try {
        await navigator.clipboard.writeText(url);
        copyBtn.classList.add('copied');
        copyBtn.querySelector('.copy-text').textContent = 'Kopyalandı!';
        copyBtn.querySelector('.copy-icon').textContent = '✅';
        showToast('Link panoya kopyalandı!');
        setTimeout(() => {
            copyBtn.classList.remove('copied');
            copyBtn.querySelector('.copy-text').textContent = 'Kopyala';
            copyBtn.querySelector('.copy-icon').textContent = '📋';
        }, 2000);
    } catch {
        // Fallback
        const input = document.createElement('input');
        input.value = url;
        document.body.appendChild(input);
        input.select();
        document.execCommand('copy');
        document.body.removeChild(input);
        showToast('Link panoya kopyalandı!');
    }
});

// ── QR Code Modal ──
qrBtn.addEventListener('click', () => {
    if (!currentShortCode) return;
    const qrUrl = `/api/v1/urls/${currentShortCode}/qr?size=300`;
    qrImage.src = qrUrl;
    downloadQrBtn.href = qrUrl;
    downloadQrBtn.download = `qr-${currentShortCode}.png`;
    qrModal.hidden = false;
});

closeQrModal.addEventListener('click', () => {
    qrModal.hidden = true;
});

qrModal.addEventListener('click', (e) => {
    if (e.target === qrModal) {
        qrModal.hidden = true;
    }
});

// ── Options Toggle ──
optionsToggle.addEventListener('click', () => {
    const isOpen = !optionsPanel.hidden;
    optionsPanel.hidden = isOpen;
    optionsToggle.classList.toggle('open', !isOpen);
});

// ════════════════════════════════════════════════════════
//  ANALYTICS
// ════════════════════════════════════════════════════════

analyticsBtn.addEventListener('click', fetchAnalytics);
analyticsInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') fetchAnalytics();
});

async function fetchAnalytics() {
    const code = analyticsInput.value.trim();
    if (!code) {
        showToast('Lütfen bir kısa kod girin.');
        analyticsInput.focus();
        return;
    }

    analyticsResult.hidden = true;

    try {
        const response = await fetch(`${API_BASE}/urls/${code}/stats`);
        if (!response.ok) {
            const error = await response.json();
            showToast(error.message || 'Link bulunamadı.');
            return;
        }

        const data = await response.json();
        renderAnalytics(data);
    } catch (err) {
        showToast('Sunucuya bağlanılamadı.');
        console.error('Analytics error:', err);
    }
}

function renderAnalytics(data) {
    document.getElementById('analyticsCode').textContent = data.shortCode;

    const statusBadge = document.getElementById('analyticsStatus');
    statusBadge.textContent = data.active ? 'Aktif' : 'Süresi Dolmuş';
    statusBadge.className = 'status-badge ' + (data.active ? 'active' : 'expired');

    document.getElementById('analyticsOriginalUrl').textContent = data.originalUrl;
    document.getElementById('analyticsTotalClicks').textContent = data.totalClicks.toLocaleString('tr-TR');
    document.getElementById('analyticsUniqueVisitors').textContent = data.uniqueVisitors.toLocaleString('tr-TR');
    document.getElementById('analyticsCreatedAt').textContent = formatDateShort(data.createdAt);
    document.getElementById('analyticsLastAccess').textContent = data.lastAccessedAt ? formatDateShort(data.lastAccessedAt) : '—';

    // Recent clicks table
    const tbody = document.getElementById('clicksTableBody');
    tbody.innerHTML = '';

    if (data.recentClicks && data.recentClicks.length > 0) {
        data.recentClicks.forEach(click => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${formatDateTime(click.clickedAt)}</td>
                <td>${click.ipAddress || '—'}</td>
                <td title="${escapeHtml(click.userAgent || '')}">${shortenUA(click.userAgent)}</td>
                <td>${click.referer || 'Doğrudan'}</td>
            `;
            tbody.appendChild(tr);
        });
    } else {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td colspan="4" style="text-align:center; color:var(--text-muted); padding:1.5rem;">Henüz tıklama yok</td>';
        tbody.appendChild(tr);
    }

    // Top referers
    const referersSection = document.getElementById('referersSection');
    const referersList = document.getElementById('referersList');

    if (data.topReferers && Object.keys(data.topReferers).length > 0) {
        referersSection.hidden = false;
        referersList.innerHTML = '';
        for (const [source, count] of Object.entries(data.topReferers)) {
            const item = document.createElement('div');
            item.className = 'referer-item';
            item.innerHTML = `
                <span class="referer-source">${escapeHtml(source)}</span>
                <span class="referer-count">${count}</span>
            `;
            referersList.appendChild(item);
        }
    } else {
        referersSection.hidden = true;
    }

    analyticsResult.hidden = false;
}

// ════════════════════════════════════════════════════════
//  RECENT LINKS
// ════════════════════════════════════════════════════════

function renderRecentList() {
    const recent = getRecentUrls();
    recentList.innerHTML = '';

    if (recent.length === 0) {
        recentList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p>Henüz link oluşturmadınız</p>
            </div>
        `;
        return;
    }

    recent.forEach(item => {
        const div = document.createElement('div');
        div.className = 'recent-item';
        const fullUrl = window.location.origin + '/' + item.shortCode;
        div.innerHTML = `
            <div class="recent-info">
                <a href="${fullUrl}" class="recent-short" target="_blank">${fullUrl}</a>
                <div class="recent-original" title="${escapeHtml(item.originalUrl)}">${item.originalUrl}</div>
            </div>
            <div class="recent-actions">
                <button class="btn-sm" onclick="copyToClipboard('${fullUrl}')">Kopyala</button>
                <button class="btn-sm" onclick="viewStats('${item.shortCode}')">İstatistik</button>
            </div>
        `;
        recentList.appendChild(div);
    });
}

// ── Global functions for inline onclick ──
window.copyToClipboard = async function(url) {
    try {
        await navigator.clipboard.writeText(url);
        showToast('Link panoya kopyalandı!');
    } catch {
        showToast('Kopyalama başarısız.');
    }
};

window.viewStats = function(code) {
    // Switch to analytics tab
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    document.querySelector('[data-tab="analytics"]').classList.add('active');
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.getElementById('tab-analytics').classList.add('active');

    analyticsInput.value = code;
    fetchAnalytics();
};

// ════════════════════════════════════════════════════════
//  STATS COUNTER
// ════════════════════════════════════════════════════════

function updateStats() {
    const recent = getRecentUrls();
    const statLinks = document.getElementById('statLinks');
    if (statLinks) {
        animateCounter(statLinks, recent.length);
    }
}

function animateCounter(element, target) {
    const current = parseInt(element.textContent) || 0;
    if (current === target) return;

    const duration = 500;
    const start = performance.now();

    function tick(now) {
        const elapsed = now - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
        element.textContent = Math.round(current + (target - current) * eased);
        if (progress < 1) requestAnimationFrame(tick);
    }

    requestAnimationFrame(tick);
}

// ════════════════════════════════════════════════════════
//  TOAST
// ════════════════════════════════════════════════════════

let toastTimeout;
function showToast(msg) {
    toastMessage.textContent = msg;
    toast.hidden = false;
    // Force reflow
    toast.offsetHeight;
    toast.classList.add('show');

    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => { toast.hidden = true; }, 400);
    }, 2500);
}

// ════════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════════

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('tr-TR', {
        day: 'numeric', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function formatDateShort(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' }) +
        ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('tr-TR') + ' ' +
        d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function shortenUA(ua) {
    if (!ua) return '—';
    if (ua.includes('Chrome')) return '🌐 Chrome';
    if (ua.includes('Firefox')) return '🦊 Firefox';
    if (ua.includes('Safari')) return '🧭 Safari';
    if (ua.includes('Edge')) return '🔷 Edge';
    if (ua.includes('curl')) return '💻 cURL';
    return ua.substring(0, 30) + '...';
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ── Init ──
updateStats();
