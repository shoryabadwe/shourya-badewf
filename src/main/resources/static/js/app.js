/**
 * TECHPULSE — Discover. Participate. Achieve.
 * Course Code: 2113611 (Full Stack Java Programming)
 * Vanilla JavaScript Frontend Controller communicating with Java Spring Boot REST APIs
 */

(function () {
    'use strict';

    // Application State
    const state = {
        currentUser: {
            authenticated: false,
            id: null,
            fullName: null,
            email: null,
            collegeOrInstitution: null,
            role: null
        },
        csrfToken: '',
        currentPage: 0,
        pageSize: 9,
        lastActiveView: 'explore',
        adminEventsCache: [],
        confirmCallback: null
    };

    // =========================================================================
    // 1. Theme Management (Bright Mode & Night Mode + System Preference)
    // =========================================================================
    const THEME_STORAGE_KEY = 'techpulse_theme';

    function initTheme() {
        const saved = localStorage.getItem(THEME_STORAGE_KEY);
        let initialTheme = 'bright';
        if (saved === 'bright' || saved === 'night') {
            initialTheme = saved;
        } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            initialTheme = 'night';
        }
        applyTheme(initialTheme);
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const iconContainer = document.getElementById('theme-icon-container');
        const labelText = document.getElementById('theme-label-text');
        if (!iconContainer || !labelText) return;

        if (theme === 'night') {
            // Show Sun icon to switch to Bright mode
            iconContainer.innerHTML = '<svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>';
            labelText.textContent = 'Bright Mode';
        } else {
            // Show Moon icon to switch to Night mode
            iconContainer.innerHTML = '<svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>';
            labelText.textContent = 'Night Mode';
        }
    }

    function toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme') || 'bright';
        const next = current === 'bright' ? 'night' : 'bright';
        localStorage.setItem(THEME_STORAGE_KEY, next);
        applyTheme(next);
    }

    // =========================================================================
    // 2. CSRF Token & Fetch API Helper
    // =========================================================================
    function readCsrfCookie() {
        const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : state.csrfToken;
    }

    async function apiFetch(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = Object.assign({ 'Accept': 'application/json' }, options.headers || {});

        if (method !== 'GET' && method !== 'HEAD') {
            let token = readCsrfCookie();
            if (!token) {
                try {
                    const csrfRes = await fetch('/api/csrf', { credentials: 'same-origin' });
                    if (csrfRes.ok) {
                        const csrfData = await csrfRes.json();
                        token = csrfData.token || '';
                        state.csrfToken = token;
                    }
                } catch (_) {
                    // Continue
                }
            }
            if (token) {
                headers['X-XSRF-TOKEN'] = token;
            }
            if (options.body && !headers['Content-Type']) {
                headers['Content-Type'] = 'application/json';
            }
        }

        const response = await fetch(url, Object.assign({}, options, {
            method,
            headers,
            credentials: 'same-origin'
        }));

        if (!response.ok) {
            let errorMessage = 'Request failed with HTTP ' + response.status;
            try {
                const errJson = await response.json();
                if (errJson && errJson.error) {
                    errorMessage = errJson.error;
                }
            } catch (_) {
                // Ignore parse error
            }
            const err = new Error(errorMessage);
            err.status = response.status;
            throw err;
        }

        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }
        return response.text();
    }

    // =========================================================================
    // 3. Formatting & Safe DOM Utilities
    // =========================================================================
    function escapeHtml(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatIstDateTime(isoString) {
        if (!isoString) return 'Not provided';
        try {
            const dt = new Date(isoString);
            if (isNaN(dt.getTime())) return 'Not provided';
            return new Intl.DateTimeFormat('en-IN', {
                timeZone: 'Asia/Kolkata',
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            }).format(dt) + ' IST';
        } catch (_) {
            return String(isoString);
        }
    }

    function toDatetimeLocalInput(isoString) {
        if (!isoString) return '';
        try {
            const dt = new Date(isoString);
            if (isNaN(dt.getTime())) return '';
            // Convert to Asia/Kolkata (+05:30) wall time for datetime-local input
            const parts = new Intl.DateTimeFormat('en-CA', {
                timeZone: 'Asia/Kolkata',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            }).formatToParts(dt);
            const map = {};
            parts.forEach(p => { map[p.type] = p.value; });
            const hour = map.hour === '24' ? '00' : map.hour;
            return `${map.year}-${map.month}-${map.day}T${hour}:${map.minute}`;
        } catch (_) {
            return '';
        }
    }

    function fromDatetimeLocalToIsoIst(localValue) {
        if (!localValue || !localValue.trim()) return null;
        return localValue.trim() + ':00+05:30';
    }

    function showToast(message, isError = false) {
        const container = document.getElementById('toast-container');
        if (!container) return;
        const item = document.createElement('div');
        item.className = 'toast-item' + (isError ? ' error' : '');
        item.textContent = message;
        container.appendChild(item);
        setTimeout(() => {
            if (item.parentNode) item.parentNode.removeChild(item);
        }, 4200);
    }

    function openConfirmModal(title, message, onConfirm) {
        const modal = document.getElementById('confirm-modal');
        const titleEl = document.getElementById('confirm-modal-title');
        const msgEl = document.getElementById('confirm-modal-message');
        if (!modal || !titleEl || !msgEl) return;

        titleEl.textContent = title;
        msgEl.textContent = message;
        state.confirmCallback = onConfirm;
        modal.classList.remove('hidden');
    }

    function closeConfirmModal() {
        const modal = document.getElementById('confirm-modal');
        if (modal) modal.classList.add('hidden');
        state.confirmCallback = null;
    }

    // =========================================================================
    // 4. Authentication & Header State
    // =========================================================================
    async function loadCurrentUser() {
        try {
            const profile = await apiFetch('/api/auth/me');
            state.currentUser = profile;
            if (profile.csrfToken) {
                state.csrfToken = profile.csrfToken;
            }
        } catch (_) {
            state.currentUser = { authenticated: false };
        }
        renderHeaderAuthControls();
    }

    function renderHeaderAuthControls() {
        const container = document.getElementById('auth-header-controls');
        if (!container) return;

        if (state.currentUser && state.currentUser.authenticated) {
            const roleLabel = state.currentUser.role === 'ADMIN' ? 'ADMIN' : 'STUDENT';
            container.innerHTML = `
                <span class="user-session-label">${escapeHtml(state.currentUser.fullName)} · ${roleLabel}</span>
                <button type="button" class="btn btn-secondary btn-sm" id="btn-logout">Logout</button>
            `;
            const logoutBtn = document.getElementById('btn-logout');
            if (logoutBtn) {
                logoutBtn.addEventListener('click', handleLogout);
            }
        } else {
            container.innerHTML = `<a href="#auth" class="btn btn-primary btn-sm">Login / Register</a>`;
        }
    }

    async function handleLogout() {
        try {
            await apiFetch('/api/auth/logout', { method: 'POST' });
            showToast('Logged out successfully.');
            await loadCurrentUser();
            window.location.hash = '#explore';
            loadExploreEvents();
        } catch (err) {
            showToast(err.message, true);
        }
    }

    // =========================================================================
    // 5. View Routing (Explore, Details, Saved, Activity, Auth, Admin)
    // =========================================================================
    function handleRouteChange() {
        const rawHash = window.location.hash || '#explore';
        const sections = document.querySelectorAll('.view-section');
        sections.forEach(s => s.classList.add('hidden'));

        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(l => l.classList.remove('active'));

        if (rawHash.startsWith('#event/')) {
            const idStr = rawHash.replace('#event/', '').trim();
            const eventId = parseInt(idStr, 10);
            document.getElementById('view-details').classList.remove('hidden');
            const exploreLink = document.querySelector('.nav-link[data-nav="explore"]');
            if (exploreLink) exploreLink.classList.add('active');
            if (!isNaN(eventId)) {
                loadEventDetails(eventId);
            }
            return;
        }

        const viewName = rawHash.replace('#', '') || 'explore';
        const activeLink = document.querySelector(`.nav-link[data-nav="${viewName}"]`);
        if (activeLink) activeLink.classList.add('active');

        if (viewName === 'saved') {
            state.lastActiveView = 'saved';
            document.getElementById('view-saved').classList.remove('hidden');
            loadSavedEventsView();
        } else if (viewName === 'activity') {
            state.lastActiveView = 'activity';
            document.getElementById('view-activity').classList.remove('hidden');
            loadActivityView();
        } else if (viewName === 'auth') {
            document.getElementById('view-auth').classList.remove('hidden');
        } else if (viewName === 'admin') {
            state.lastActiveView = 'admin';
            document.getElementById('view-admin').classList.remove('hidden');
            loadAdminDashboardView();
        } else {
            state.lastActiveView = 'explore';
            document.getElementById('view-explore').classList.remove('hidden');
            loadExploreEvents();
        }
    }

    // =========================================================================
    // 6. Live Source Status & Refresh
    // =========================================================================
    async function loadSourceStatus() {
        try {
            const status = await apiFetch('/api/sources/status');
            renderSourceStatusStrip(status);
            renderAdminSourceBox(status);
        } catch (err) {
            const headline = document.getElementById('source-status-headline');
            if (headline) headline.textContent = 'Source status unavailable';
        }
    }

    function renderSourceStatusStrip(status) {
        const dot = document.getElementById('source-status-dot');
        const headline = document.getElementById('source-status-headline');
        const syncTime = document.getElementById('source-last-sync');
        const detail = document.getElementById('source-status-detail');
        if (!dot || !headline || !syncTime || !detail) return;

        dot.className = 'status-dot';
        if (status.status === 'OK') {
            dot.classList.add('ok');
            headline.textContent = `Live Source Active (${status.sourceName})`;
        } else if (status.status === 'LIVE_SOURCE_NOT_CONFIGURED') {
            dot.classList.add('warn');
            headline.textContent = 'Live source not configured';
        } else if (status.status === 'STALE_CACHE') {
            dot.classList.add('warn');
            headline.textContent = 'Stale Data Cache Active (Outage Fallback)';
        } else {
            dot.classList.add('danger');
            headline.textContent = `Source Status: ${status.status}`;
        }

        syncTime.textContent = status.lastSuccessAt
            ? `Last successful refresh: ${formatIstDateTime(status.lastSuccessAt)}`
            : 'Last successful refresh: Never (Demo & Manual records active)';

        detail.textContent = status.statusMessage || '';
    }

    async function handleRefreshSource() {
        try {
            showToast('Triggering event source refresh...');
            const updatedStatus = await apiFetch('/api/sources/refresh', { method: 'POST' });
            renderSourceStatusStrip(updatedStatus);
            renderAdminSourceBox(updatedStatus);
            showToast(updatedStatus.statusMessage || 'Source refresh completed.');
            loadExploreEvents();
        } catch (err) {
            if (err.status === 401) {
                showToast('Please sign in to trigger a live source refresh.', true);
                window.location.hash = '#auth';
            } else {
                showToast(err.message, true);
            }
        }
    }

    // =========================================================================
    // 7. Explore Events Discovery, Combined Filters & Pagination
    // =========================================================================
    async function loadExploreEvents() {
        const container = document.getElementById('explore-events-container');
        const countEl = document.getElementById('explore-results-count');
        const windowRangeEl = document.getElementById('server-window-range');
        if (!container) return;

        container.innerHTML = `<div class="empty-state-panel"><p class="tabular-nums">Loading upcoming technology events from Java server...</p></div>`;

        const search = document.getElementById('filter-search').value.trim();
        const dateWindow = document.getElementById('filter-date-window').value;
        const sortBy = document.getElementById('filter-sort').value;
        const city = document.getElementById('filter-city').value;
        const topic = document.getElementById('filter-topic').value;
        const eventType = document.getElementById('filter-type').value;
        const mode = document.getElementById('filter-mode').value;
        const cost = document.getElementById('filter-cost').value;
        const maxPrice = document.getElementById('filter-max-price').value.trim();

        const params = new URLSearchParams({
            city,
            topic,
            eventType,
            mode,
            cost,
            dateWindow,
            sortBy,
            page: String(state.currentPage),
            size: String(state.pageSize)
        });
        if (search) params.set('search', search);
        if (maxPrice !== '') params.set('maxPrice', maxPrice);

        try {
            const data = await apiFetch('/api/events?' + params.toString());
            if (windowRangeEl && data.windowStart && data.windowEnd) {
                windowRangeEl.textContent = `${formatIstDateTime(data.windowStart)} → ${formatIstDateTime(data.windowEnd)}`;
            }
            if (countEl) {
                countEl.textContent = `Showing ${data.content.length} of ${data.totalElements} matching event(s) in [${data.activeDateWindow}]`;
            }

            if (!data.content || data.content.length === 0) {
                container.innerHTML = `
                    <div class="empty-state-panel">
                        <h3 class="subsection-title">No Events Match Your Current Filters</h3>
                        <p class="section-subtitle" style="margin: 0.5rem auto 1rem;">
                            Try broadening your date window (e.g. Next 30 Days or All Upcoming) or clearing city/topic filters.
                        </p>
                        <button type="button" class="btn btn-primary btn-sm" id="btn-empty-reset">Reset All Filters</button>
                    </div>
                `;
                const emptyReset = document.getElementById('btn-empty-reset');
                if (emptyReset) emptyReset.addEventListener('click', resetExploreFilters);
                renderPagination(0, 1);
                return;
            }

            container.innerHTML = data.content.map(ev => renderEventCardHtml(ev)).join('');
            attachCardListeners(container);
            renderPagination(data.page, data.totalPages);
        } catch (err) {
            container.innerHTML = `
                <div class="empty-state-panel">
                    <h3 class="subsection-title">Error Loading Events</h3>
                    <p class="section-subtitle">${escapeHtml(err.message)}</p>
                </div>
            `;
        }
    }

    function renderEventCardHtml(ev, extraConflictNoticeHtml = '') {
        const bookmarkBtnClass = ev.bookmarked ? 'btn btn-sm btn-bookmarked' : 'btn btn-sm btn-secondary';
        const bookmarkLabel = ev.bookmarked ? 'Saved ✓' : 'Save Event';

        let statusBanner = '';
        if (ev.cancelled) {
            statusBanner = `<div class="card-status-notice">STATUS: CANCELLED BY ORGANISER</div>`;
        } else if (ev.registrationClosed) {
            statusBanner = `<div class="card-status-notice warn">STATUS: REGISTRATION CLOSED</div>`;
        }

        const deadlineDisplay = ev.registrationDeadline
            ? formatIstDateTime(ev.registrationDeadline)
            : 'Not provided';

        const regLinkHtml = ev.registrationUrl
            ? `<a href="${escapeHtml(ev.registrationUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-secondary" title="Opens external registration page (does not auto-mark participation)">Official Link &nearr;</a>`
            : '';

        return `
            <article class="event-card ${ev.cancelled ? 'cancelled-card' : ''}" data-event-id="${ev.id}">
                <div>
                    <div class="card-meta-kicker">
                        <span>${escapeHtml(ev.topic)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.eventType)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.mode)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.dataOriginLabel)}</span>
                    </div>
                    <a href="#event/${ev.id}" class="card-title-link">${escapeHtml(ev.title)}</a>
                    <div class="card-organizer">Organiser: ${escapeHtml(ev.organizer)}</div>
                    ${statusBanner}
                    ${extraConflictNoticeHtml}
                </div>

                <div class="card-data-rows">
                    <div class="card-data-row">
                        <span class="card-data-key">Starts (IST)</span>
                        <span class="card-data-val tabular-nums">${escapeHtml(formatIstDateTime(ev.startTime))}</span>
                    </div>
                    <div class="card-data-row">
                        <span class="card-data-key">Countdown</span>
                        <span class="card-data-val countdown-text tabular-nums">${escapeHtml(ev.countdownLabel)}</span>
                    </div>
                    <div class="card-data-row">
                        <span class="card-data-key">Location / Mode</span>
                        <span class="card-data-val">${escapeHtml(ev.city)} (${escapeHtml(ev.mode)})</span>
                    </div>
                    <div class="card-data-row">
                        <span class="card-data-key">Cost</span>
                        <span class="card-data-val price-text tabular-nums">${escapeHtml(ev.priceDisplay)}</span>
                    </div>
                    <div class="card-data-row">
                        <span class="card-data-key">Reg. Deadline</span>
                        <span class="card-data-val tabular-nums">${escapeHtml(deadlineDisplay)}</span>
                    </div>
                </div>

                <div class="card-actions">
                    <div class="card-actions-left">
                        <button type="button" class="${bookmarkBtnClass} js-bookmark-toggle" data-event-id="${ev.id}" data-bookmarked="${ev.bookmarked}">
                            ${bookmarkLabel}
                        </button>
                        <a href="#event/${ev.id}" class="btn btn-sm btn-primary">Details</a>
                    </div>
                    <div class="card-actions-right">
                        ${regLinkHtml}
                    </div>
                </div>
            </article>
        `;
    }

    function attachCardListeners(container) {
        const bookmarkBtns = container.querySelectorAll('.js-bookmark-toggle');
        bookmarkBtns.forEach(btn => {
            btn.addEventListener('click', async () => {
                const eventId = parseInt(btn.getAttribute('data-event-id'), 10);
                const currentlyBookmarked = btn.getAttribute('data-bookmarked') === 'true';
                await toggleBookmark(eventId, currentlyBookmarked);
            });
        });
    }

    function renderPagination(currentPage, totalPages) {
        const bar = document.getElementById('explore-pagination');
        if (!bar) return;
        if (totalPages <= 1) {
            bar.innerHTML = '';
            return;
        }
        bar.innerHTML = `
            <button type="button" class="btn btn-secondary btn-sm" id="page-prev" ${currentPage <= 0 ? 'disabled' : ''}>&larr; Previous</button>
            <span class="tabular-nums">Page ${currentPage + 1} of ${totalPages}</span>
            <button type="button" class="btn btn-secondary btn-sm" id="page-next" ${currentPage + 1 >= totalPages ? 'disabled' : ''}>Next &rarr;</button>
        `;
        const prev = document.getElementById('page-prev');
        const next = document.getElementById('page-next');
        if (prev && currentPage > 0) {
            prev.addEventListener('click', () => {
                state.currentPage = currentPage - 1;
                loadExploreEvents();
            });
        }
        if (next && currentPage + 1 < totalPages) {
            next.addEventListener('click', () => {
                state.currentPage = currentPage + 1;
                loadExploreEvents();
            });
        }
    }

    function resetExploreFilters() {
        document.getElementById('filter-search').value = '';
        document.getElementById('filter-date-window').value = 'NEXT_30_DAYS';
        document.getElementById('filter-sort').value = 'SOONEST';
        document.getElementById('filter-city').value = 'ALL';
        document.getElementById('filter-topic').value = 'ALL';
        document.getElementById('filter-type').value = 'ALL';
        document.getElementById('filter-mode').value = 'ALL';
        document.getElementById('filter-cost').value = 'ALL';
        document.getElementById('filter-max-price').value = '';
        state.currentPage = 0;
        loadExploreEvents();
    }

    // =========================================================================
    // 8. Event Details, .ics Download, Copy Link & WhatsApp Share
    // =========================================================================
    async function loadEventDetails(eventId) {
        const container = document.getElementById('event-details-container');
        if (!container) return;
        container.innerHTML = `<p class="tabular-nums">Loading event details...</p>`;

        try {
            const ev = await apiFetch(`/api/events/${eventId}`);
            const appEventUrl = `${window.location.origin}/#event/${ev.id}`;
            const whatsappText = encodeURIComponent(
                `Check out "${ev.title}" (${ev.eventType} · ${ev.city}) starting on ${formatIstDateTime(ev.startTime)} on TECHPULSE: ${appEventUrl}`
            );
            const whatsappUrl = `https://wa.me/?text=${whatsappText}`;
            const icsDownloadUrl = `/api/events/${ev.id}/calendar.ics?appUrl=${encodeURIComponent(appEventUrl)}`;

            const isCompetitive = ev.eventType === 'HACKATHON' || ev.eventType === 'COMPETITION';
            const currentStatus = ev.participationStatus || 'SAVED';
            const currentOutcome = ev.competitionOutcome || (isCompetitive ? 'PENDING' : 'NONE');

            const endTimeDisplay = ev.endTime
                ? formatIstDateTime(ev.endTime)
                : 'Not provided (Calendar export assumes 2-hour duration)';

            container.innerHTML = `
                <div class="details-header">
                    <div class="card-meta-kicker">
                        <span>${escapeHtml(ev.topic)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.eventType)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.mode)}</span>
                        <span class="meta-separator">·</span>
                        <span>${escapeHtml(ev.dataOriginLabel)}</span>
                    </div>
                    <h1 id="details-heading" class="details-title">${escapeHtml(ev.title)}</h1>
                    <p class="card-organizer">Organised by <strong>${escapeHtml(ev.organizer)}</strong> ·Countdown: <strong class="countdown-text tabular-nums">${escapeHtml(ev.countdownLabel)}</strong></p>
                    ${ev.cancelled ? '<div class="card-status-notice">THIS EVENT HAS BEEN MARKED AS CANCELLED</div>' : ''}
                    ${ev.registrationClosed ? '<div class="card-status-notice warn">OFFICIAL REGISTRATION IS CLOSED</div>' : ''}

                    <div class="details-actions-bar">
                        <button type="button" class="btn ${ev.bookmarked ? 'btn-bookmarked' : 'btn-primary'}" id="details-btn-bookmark">
                            ${ev.bookmarked ? 'Saved in Bookmarks ✓ (Click to Remove)' : 'Save Event to Bookmarks'}
                        </button>
                        ${ev.registrationUrl ? `
                            <a href="${escapeHtml(ev.registrationUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">
                                Open Official Registration &nearr;
                            </a>
                        ` : '<span class="btn btn-secondary" aria-disabled="true">Official Registration Link: Not provided</span>'}
                        <a href="${escapeHtml(icsDownloadUrl)}" download="techpulse-event-${ev.id}.ics" class="btn btn-secondary">
                            Add to Calendar (.ics)
                        </a>
                        <button type="button" class="btn btn-secondary" id="details-btn-copy-link">
                            Copy Event Link
                        </button>
                        <a href="${escapeHtml(whatsappUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">
                            Share via WhatsApp
                        </a>
                    </div>
                    <p class="badge-disclaimer-text mt-xs">
                        Note: Opening an external registration link does not automatically mark you as registered. Record your participation status below.
                    </p>
                </div>

                <div class="details-meta-grid">
                    <div class="details-meta-item">
                        <span class="meta-label">Start Date &amp; Time (IST)</span>
                        <span class="meta-value tabular-nums">${escapeHtml(formatIstDateTime(ev.startTime))}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">End Date &amp; Time (IST)</span>
                        <span class="meta-value tabular-nums">${escapeHtml(endTimeDisplay)}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Registration Deadline</span>
                        <span class="meta-value tabular-nums">${escapeHtml(formatIstDateTime(ev.registrationDeadline))}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">City / Mode</span>
                        <span class="meta-value">${escapeHtml(ev.city)} · ${escapeHtml(ev.mode)}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Venue Address / Online Platform</span>
                        <span class="meta-value">${escapeHtml(ev.venueOrPlatform || 'Not provided')}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Price &amp; Currency</span>
                        <span class="meta-value tabular-nums">${escapeHtml(ev.priceDisplay)}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Eligibility</span>
                        <span class="meta-value">${escapeHtml(ev.eligibility || 'Not provided')}</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Source Name &amp; Origin</span>
                        <span class="meta-value">${escapeHtml(ev.sourceName)} (${escapeHtml(ev.dataOriginLabel)})</span>
                    </div>
                    <div class="details-meta-item">
                        <span class="meta-label">Last Fetched / Updated</span>
                        <span class="meta-value tabular-nums">${escapeHtml(formatIstDateTime(ev.lastFetchedAt))}</span>
                    </div>
                </div>

                <div class="details-description-box">
                    <h2 class="subsection-title">Full Event Description</h2>
                    <div class="details-description-text" id="details-safe-description"></div>
                </div>

                <div class="participation-quick-panel">
                    <h2 class="subsection-title">Record Self-Reported Participation Status</h2>
                    <p class="section-subtitle">
                        Update your progress for this event (Saved = 0 credits, Registered = 5 credits, Attended = 20 total credits, Won Competition = 50 total credits).
                    </p>
                    <form id="details-participation-form" class="quick-participation-form">
                        <div class="form-group">
                            <label for="details-part-status" class="form-label">Participation Status</label>
                            <select id="details-part-status" class="form-select">
                                <option value="SAVED" ${currentStatus === 'SAVED' ? 'selected' : ''}>Saved (0 Credits)</option>
                                <option value="REGISTERED" ${currentStatus === 'REGISTERED' ? 'selected' : ''}>Registered (5 Credits)</option>
                                <option value="ATTENDED" ${currentStatus === 'ATTENDED' ? 'selected' : ''}>Attended (20 Credits)</option>
                            </select>
                        </div>
                        ${isCompetitive ? `
                            <div class="form-group">
                                <label for="details-part-outcome" class="form-label">Competition Outcome (Requires Attended)</label>
                                <select id="details-part-outcome" class="form-select">
                                    <option value="PENDING" ${currentOutcome === 'PENDING' ? 'selected' : ''}>Pending</option>
                                    <option value="WON" ${currentOutcome === 'WON' ? 'selected' : ''}>Won (50 Total Credits)</option>
                                    <option value="NOT_WON" ${currentOutcome === 'NOT_WON' ? 'selected' : ''}>Not Won (20 Total Credits)</option>
                                </select>
                            </div>
                        ` : ''}
                        <div class="form-group" style="flex:1; min-width:220px;">
                            <label for="details-part-note" class="form-label">Self-Reported Note / Team / Certificate Ref</label>
                            <input type="text" id="details-part-note" class="form-input" maxlength="400" placeholder="Optional note for faculty verification" />
                        </div>
                        <button type="submit" class="btn btn-primary">Save Participation Status</button>
                    </form>
                </div>
            `;

            // Safely assign description using textContent so external strings can never execute HTML/JS
            const safeDescEl = document.getElementById('details-safe-description');
            if (safeDescEl) {
                safeDescEl.textContent = ev.description || 'Not provided';
            }

            // Attach listeners
            const bookmarkBtn = document.getElementById('details-btn-bookmark');
            if (bookmarkBtn) {
                bookmarkBtn.addEventListener('click', async () => {
                    await toggleBookmark(ev.id, ev.bookmarked);
                    loadEventDetails(ev.id);
                });
            }

            const copyBtn = document.getElementById('details-btn-copy-link');
            if (copyBtn) {
                copyBtn.addEventListener('click', async () => {
                    try {
                        await navigator.clipboard.writeText(appEventUrl);
                        showToast('Event link copied to clipboard: ' + appEventUrl);
                    } catch (_) {
                        showToast('Event link: ' + appEventUrl);
                    }
                });
            }

            const partForm = document.getElementById('details-participation-form');
            if (partForm) {
                partForm.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    if (!state.currentUser.authenticated) {
                        showToast('Please sign in to record participation.', true);
                        window.location.hash = '#auth';
                        return;
                    }
                    const statusVal = document.getElementById('details-part-status').value;
                    const outcomeEl = document.getElementById('details-part-outcome');
                    const outcomeVal = outcomeEl ? outcomeEl.value : 'NONE';
                    const noteVal = document.getElementById('details-part-note').value;

                    try {
                        await apiFetch('/api/participations', {
                            method: 'POST',
                            body: JSON.stringify({
                                eventId: ev.id,
                                status: statusVal,
                                outcome: outcomeVal,
                                studentNote: noteVal
                            })
                        });
                        showToast('Participation status updated (Self-Reported).');
                        loadEventDetails(ev.id);
                    } catch (err) {
                        showToast(err.message, true);
                    }
                });
            }
        } catch (err) {
            container.innerHTML = `<div class="empty-state-panel"><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    // =========================================================================
    // 9. Bookmarks & Schedule Conflict Detection
    // =========================================================================
    async function toggleBookmark(eventId, currentlyBookmarked) {
        if (!state.currentUser.authenticated) {
            showToast('Please sign in to save events to your bookmarks.', true);
            window.location.hash = '#auth';
            return;
        }
        try {
            if (currentlyBookmarked) {
                await apiFetch(`/api/bookmarks/${eventId}`, { method: 'DELETE' });
                showToast('Removed event from your saved bookmarks.');
            } else {
                await apiFetch(`/api/bookmarks/${eventId}`, { method: 'POST' });
                showToast('Event saved to your bookmarks.');
            }
            if (state.lastActiveView === 'saved') {
                loadSavedEventsView();
            } else if (state.lastActiveView === 'explore') {
                loadExploreEvents();
            }
        } catch (err) {
            showToast(err.message, true);
        }
    }

    async function loadSavedEventsView() {
        const conflictsBanner = document.getElementById('saved-conflicts-banner');
        const upcomingContainer = document.getElementById('saved-upcoming-container');
        const pastContainer = document.getElementById('saved-past-container');
        if (!upcomingContainer || !pastContainer) return;

        if (!state.currentUser.authenticated) {
            conflictsBanner.innerHTML = '';
            upcomingContainer.innerHTML = `
                <div class="empty-state-panel">
                    <h3 class="subsection-title">Sign In Required</h3>
                    <p class="section-subtitle">Please sign in to view your saved events and schedule conflict warnings.</p>
                    <div class="mt-md"><a href="#auth" class="btn btn-primary btn-sm">Sign In / Register</a></div>
                </div>
            `;
            pastContainer.innerHTML = '';
            return;
        }

        try {
            const overview = await apiFetch('/api/bookmarks');

            // Render conflict warnings banner
            let bannerHtml = '';
            if (overview.conflicts && overview.conflicts.length > 0) {
                bannerHtml += `
                    <div class="conflict-alert-box" role="alert">
                        <div class="conflict-alert-title">
                            SCHEDULE OVERLAP WARNING (${overview.conflicts.length} Clash${overview.conflicts.length > 1 ? 'es' : ''} Detected)
                        </div>
                        <ul class="conflict-list">
                            ${overview.conflicts.map(c => `<li>${escapeHtml(c.explanation)}</li>`).join('')}
                        </ul>
                    </div>
                `;
            }
            if (overview.incompleteTimeCount > 0) {
                bannerHtml += `
                    <div class="card-status-notice warn">
                        NOTE: ${overview.incompleteTimeCount} saved event(s) do not have an official end time.
                        Marked as <strong>"Cannot check overlap"</strong> rather than inventing a schedule clash.
                    </div>
                `;
            }
            conflictsBanner.innerHTML = bannerHtml;

            // Render Upcoming Saved Events
            if (!overview.upcomingBookmarks || overview.upcomingBookmarks.length === 0) {
                upcomingContainer.innerHTML = `
                    <div class="empty-state-panel">
                        <p>No upcoming saved events yet. Browse <a href="#explore">Explore Events</a> to bookmark sessions.</p>
                    </div>
                `;
            } else {
                upcomingContainer.innerHTML = overview.upcomingBookmarks.map(item => {
                    let overlapBadge = '';
                    if (item.overlapCheckStatus === 'CONFLICT_DETECTED') {
                        overlapBadge = `<div class="card-status-notice">OVERLAP WARNING: ${escapeHtml(item.overlapCheckMessage)}</div>`;
                    } else if (item.overlapCheckStatus === 'CANNOT_CHECK_OVERLAP') {
                        overlapBadge = `<div class="card-status-notice warn">CANNOT CHECK OVERLAP: End time not provided</div>`;
                    } else if (item.overlapCheckStatus === 'NO_CONFLICT') {
                        overlapBadge = `<div class="card-status-notice ok">${escapeHtml(item.overlapCheckMessage)}</div>`;
                    }
                    return renderEventCardHtml(item.event, overlapBadge);
                }).join('');
                attachCardListeners(upcomingContainer);
            }

            // Render Past Saved Events
            if (!overview.pastBookmarks || overview.pastBookmarks.length === 0) {
                pastContainer.innerHTML = `
                    <div class="empty-state-panel">
                        <p>No past saved events in your archive.</p>
                    </div>
                `;
            } else {
                pastContainer.innerHTML = overview.pastBookmarks.map(item => {
                    const pastNote = `<div class="card-status-notice warn">PAST EVENT · Retained in your saved history</div>`;
                    return renderEventCardHtml(item.event, pastNote);
                }).join('');
                attachCardListeners(pastContainer);
            }
        } catch (err) {
            upcomingContainer.innerHTML = `<div class="empty-state-panel"><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    // =========================================================================
    // 10. My Activity, Credits, Badges & Competition Win Rate
    // =========================================================================
    async function loadActivityView() {
        const badgePanel = document.getElementById('activity-badge-panel');
        const statsGrid = document.getElementById('activity-stats-grid');
        const tableWrapper = document.getElementById('activity-history-table-wrapper');
        if (!badgePanel || !statsGrid || !tableWrapper) return;

        if (!state.currentUser.authenticated) {
            badgePanel.innerHTML = '';
            statsGrid.innerHTML = `
                <div class="empty-state-panel">
                    <h3 class="subsection-title">Sign In Required</h3>
                    <p class="section-subtitle">Please sign in to track participation credits, competition win rates, and engagement badges.</p>
                    <div class="mt-md"><a href="#auth" class="btn btn-primary btn-sm">Sign In / Register</a></div>
                </div>
            `;
            tableWrapper.innerHTML = '';
            return;
        }

        try {
            const summary = await apiFetch('/api/participations/summary');

            // 1. Engagement Badge Panel
            badgePanel.innerHTML = `
                <div class="badge-header-row">
                    <div>
                        <div class="section-kicker">APPLICATION ENGAGEMENT BADGE (NOT AN OFFICIAL CERTIFICATION)</div>
                        <div class="badge-name-display">Current Badge: ${escapeHtml(summary.currentBadge)} (${escapeHtml(summary.badgeTierRange)})</div>
                    </div>
                    <div class="tabular-nums">
                        ${summary.creditsToNextBadge > 0
                            ? `<strong>${summary.creditsToNextBadge}</strong> credits needed for <strong>${escapeHtml(summary.nextBadge)}</strong>`
                            : `<strong>Highest Tier Achieved (Champion 300+)</strong>`}
                    </div>
                </div>
                <div class="progress-track" role="progressbar" aria-valuenow="${summary.badgeProgressPercent}" aria-valuemin="0" aria-valuemax="100">
                    <div class="progress-fill" style="width: ${summary.badgeProgressPercent}%;"></div>
                </div>
                <p class="badge-disclaimer-text">
                    Tier Ladder: Explorer (0–49) · Builder (50–149) · Challenger (150–299) · Champion (300+). ${escapeHtml(summary.badgeDisclaimer)}
                </p>
            `;

            // 2. 8-Metric Dashboard Grid
            statsGrid.innerHTML = `
                <div class="stat-box">
                    <div class="stat-label">01. Events Saved</div>
                    <div class="stat-number tabular-nums">${summary.eventsSaved}</div>
                    <div class="stat-subtext">Bookmarked in H2 database</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">02. Registered Events</div>
                    <div class="stat-number tabular-nums">${summary.registeredEventsCount}</div>
                    <div class="stat-subtext">5 credits each</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">03. Attended Events</div>
                    <div class="stat-number tabular-nums">${summary.attendedEventsCount}</div>
                    <div class="stat-subtext">20 base credits each</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">04. Competitions Participated</div>
                    <div class="stat-number tabular-nums">${summary.competitionsParticipatedCount}</div>
                    <div class="stat-subtext">Attended Hackathons &amp; Competitions</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">05. Competition Wins</div>
                    <div class="stat-number tabular-nums">${summary.winsCount}</div>
                    <div class="stat-subtext">50 total credits per win</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">06. Total Credits</div>
                    <div class="stat-number tabular-nums">${summary.totalCredits}</div>
                    <div class="stat-subtext">Non-cumulative event sum</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">07. Attended (Last 30 Days)</div>
                    <div class="stat-number tabular-nums">${summary.attendedLast30DaysCount}</div>
                    <div class="stat-subtext">Rolling 30-day IST window</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">08. Competition Win Rate</div>
                    <div class="stat-number tabular-nums">${escapeHtml(summary.winRateDisplay)}</div>
                    <div class="stat-subtext" title="${escapeHtml(summary.winRateExplanation)}">
                        Numerator: ${summary.winRateNumerator} Wins / Denominator: ${summary.winRateDenominator} Completed
                    </div>
                </div>
            `;

            // 3. Activity History Table with interactive self-reported updates
            if (!summary.activityHistory || summary.activityHistory.length === 0) {
                tableWrapper.innerHTML = `
                    <div class="empty-state-panel">
                        <p>No participation history recorded yet. Save or update an event from Explore Events.</p>
                    </div>
                `;
                return;
            }

            tableWrapper.innerHTML = `
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Event Name</th>
                            <th>Type &amp; Topic</th>
                            <th>Participation Status</th>
                            <th>Competition Outcome</th>
                            <th>Verification Status</th>
                            <th>Total Credits</th>
                            <th>Update Self-Reported Record</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${summary.activityHistory.map(row => renderActivityHistoryRow(row)).join('')}
                    </tbody>
                </table>
            `;

            // Attach update buttons in activity table
            const updateButtons = tableWrapper.querySelectorAll('.js-update-participation-row');
            updateButtons.forEach(btn => {
                btn.addEventListener('click', async () => {
                    const eventId = parseInt(btn.getAttribute('data-event-id'), 10);
                    const statusSelect = document.getElementById(`act-status-${eventId}`);
                    const outcomeSelect = document.getElementById(`act-outcome-${eventId}`);
                    const status = statusSelect ? statusSelect.value : 'SAVED';
                    const outcome = outcomeSelect ? outcomeSelect.value : 'NONE';
                    try {
                        await apiFetch('/api/participations', {
                            method: 'POST',
                            body: JSON.stringify({ eventId, status, outcome })
                        });
                        showToast('Updated participation record and recalculated credits.');
                        loadActivityView();
                    } catch (err) {
                        showToast(err.message, true);
                    }
                });
            });
        } catch (err) {
            tableWrapper.innerHTML = `<div class="empty-state-panel"><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    function renderActivityHistoryRow(row) {
        const verificationText = row.verifiedByAdmin
            ? `<strong>Admin Verified ✓</strong>${row.adminVerificationNote ? `<br/><small>${escapeHtml(row.adminVerificationNote)}</small>` : ''}`
            : `<span>Self-Reported</span>${row.studentNote ? `<br/><small>${escapeHtml(row.studentNote)}</small>` : ''}`;

        const outcomeSelectHtml = row.competitiveEvent
            ? `
                <select id="act-outcome-${row.eventId}" class="form-select">
                    <option value="PENDING" ${row.outcome === 'PENDING' ? 'selected' : ''}>Pending</option>
                    <option value="WON" ${row.outcome === 'WON' ? 'selected' : ''}>Won (50 Cr)</option>
                    <option value="NOT_WON" ${row.outcome === 'NOT_WON' ? 'selected' : ''}>Not Won (20 Cr)</option>
                </select>
            `
            : `<span class="tabular-nums">N/A (Non-competitive)</span>`;

        return `
            <tr>
                <td>
                    <a href="#event/${row.eventId}"><strong>${escapeHtml(row.eventTitle)}</strong></a>
                    ${row.eventDeleted ? '<br/><small>(Archived / Removed from public explore)</small>' : ''}
                </td>
                <td>${escapeHtml(row.eventType)} · ${escapeHtml(row.eventTopic)}</td>
                <td>
                    <select id="act-status-${row.eventId}" class="form-select">
                        <option value="SAVED" ${row.status === 'SAVED' ? 'selected' : ''}>Saved (0 Cr)</option>
                        <option value="REGISTERED" ${row.status === 'REGISTERED' ? 'selected' : ''}>Registered (5 Cr)</option>
                        <option value="ATTENDED" ${row.status === 'ATTENDED' ? 'selected' : ''}>Attended (20 Cr)</option>
                    </select>
                </td>
                <td>${outcomeSelectHtml}</td>
                <td>${verificationText}</td>
                <td class="tabular-nums"><strong>${row.credits}</strong></td>
                <td>
                    <button type="button" class="btn btn-primary btn-sm js-update-participation-row" data-event-id="${row.eventId}">
                        Save Update
                    </button>
                </td>
            </tr>
        `;
    }

    // =========================================================================
    // 11. Admin Dashboard (Event CRUD, Soft Delete, Source Status, Verification)
    // =========================================================================
    async function loadAdminDashboardView() {
        const unauthBox = document.getElementById('admin-unauthorized-box');
        const authContent = document.getElementById('admin-authorized-content');
        if (!unauthBox || !authContent) return;

        if (!state.currentUser.authenticated || state.currentUser.role !== 'ADMIN') {
            unauthBox.classList.remove('hidden');
            authContent.classList.add('hidden');
            return;
        }

        unauthBox.classList.add('hidden');
        authContent.classList.remove('hidden');

        await loadSourceStatus();
        await loadAdminEventsTable();
        await loadAdminParticipationsTable();
    }

    function renderAdminSourceBox(status) {
        const box = document.getElementById('admin-source-status-box');
        if (!box || !status) return;
        box.innerHTML = `
            <div class="card-data-rows" style="border-top:none;">
                <div class="card-data-row">
                    <span class="card-data-key">Adapter Name</span>
                    <span class="card-data-val">${escapeHtml(status.sourceName)}</span>
                </div>
                <div class="card-data-row">
                    <span class="card-data-key">Status Code</span>
                    <span class="card-data-val tabular-nums">${escapeHtml(status.status)} (${status.configured ? 'Configured' : 'Live source not configured'})</span>
                </div>
                <div class="card-data-row">
                    <span class="card-data-key">Last Attempt / Last Success</span>
                    <span class="card-data-val tabular-nums">${formatIstDateTime(status.lastAttemptAt)} / ${formatIstDateTime(status.lastSuccessAt)}</span>
                </div>
                <div class="card-data-row">
                    <span class="card-data-key">Configuration Policy</span>
                    <span class="card-data-val">${escapeHtml(status.requiredEnvInstructions)}</span>
                </div>
            </div>
        `;
    }

    async function loadAdminEventsTable() {
        const wrapper = document.getElementById('admin-events-table-wrapper');
        if (!wrapper) return;
        try {
            const events = await apiFetch('/api/admin/events');
            state.adminEventsCache = events;
            wrapper.innerHTML = `
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Title &amp; Organiser</th>
                            <th>Type / Mode / City</th>
                            <th>Start Time (IST)</th>
                            <th>Cost</th>
                            <th>Origin</th>
                            <th>Status</th>
                            <th>Admin Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${events.map(ev => `
                            <tr>
                                <td class="tabular-nums">#${ev.id}</td>
                                <td>
                                    <strong>${escapeHtml(ev.title)}</strong><br/>
                                    <small>${escapeHtml(ev.organizer)}</small>
                                </td>
                                <td>${escapeHtml(ev.eventType)} · ${escapeHtml(ev.mode)} · ${escapeHtml(ev.city)}</td>
                                <td class="tabular-nums">${escapeHtml(formatIstDateTime(ev.startTime))}</td>
                                <td class="tabular-nums">${escapeHtml(ev.priceDisplay)}</td>
                                <td>${escapeHtml(ev.dataOriginLabel)}</td>
                                <td>${ev.cancelled ? '<strong>CANCELLED</strong>' : (ev.registrationClosed ? 'Reg. Closed' : 'Active')}</td>
                                <td>
                                    <div class="card-actions-left">
                                        <button type="button" class="btn btn-secondary btn-sm js-admin-edit-ev" data-id="${ev.id}">Edit</button>
                                        <button type="button" class="btn btn-secondary btn-sm js-admin-cancel-ev" data-id="${ev.id}" data-cancelled="${ev.cancelled}">
                                            ${ev.cancelled ? 'Restore' : 'Mark Cancelled'}
                                        </button>
                                        <button type="button" class="btn btn-danger btn-sm js-admin-delete-ev" data-id="${ev.id}" data-title="${escapeHtml(ev.title)}">
                                            Soft Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;

            wrapper.querySelectorAll('.js-admin-edit-ev').forEach(btn => {
                btn.addEventListener('click', () => {
                    const id = parseInt(btn.getAttribute('data-id'), 10);
                    populateAdminFormForEdit(id);
                });
            });

            wrapper.querySelectorAll('.js-admin-cancel-ev').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const id = parseInt(btn.getAttribute('data-id'), 10);
                    const currentlyCancelled = btn.getAttribute('data-cancelled') === 'true';
                    try {
                        await apiFetch(`/api/admin/events/${id}/cancel`, {
                            method: 'PATCH',
                            body: JSON.stringify({ cancelled: !currentlyCancelled })
                        });
                        showToast(`Event #${id} ${!currentlyCancelled ? 'marked as cancelled' : 'restored to active'}.`);
                        loadAdminEventsTable();
                    } catch (err) {
                        showToast(err.message, true);
                    }
                });
            });

            wrapper.querySelectorAll('.js-admin-delete-ev').forEach(btn => {
                btn.addEventListener('click', () => {
                    const id = parseInt(btn.getAttribute('data-id'), 10);
                    const title = btn.getAttribute('data-title');
                    openConfirmModal(
                        'Confirm Event Soft-Deletion',
                        `Are you sure you want to delete "${title}" (#${id})? This performs a soft-delete so the event is removed from public discovery while preserving student bookmarks and participation credits.`,
                        async () => {
                            try {
                                await apiFetch(`/api/admin/events/${id}`, { method: 'DELETE' });
                                showToast(`Event #${id} soft-deleted (history preserved).`);
                                loadAdminEventsTable();
                            } catch (err) {
                                showToast(err.message, true);
                            }
                        }
                    );
                });
            });
        } catch (err) {
            wrapper.innerHTML = `<div class="empty-state-panel"><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    function populateAdminFormForEdit(eventId) {
        const ev = state.adminEventsCache.find(e => e.id === eventId);
        if (!ev) return;

        document.getElementById('admin-form-heading').textContent = `Edit Event #${ev.id}: ${ev.title}`;
        document.getElementById('admin-btn-cancel-edit').classList.remove('hidden');
        document.getElementById('admin-event-id').value = String(ev.id);
        document.getElementById('admin-title').value = ev.title || '';
        document.getElementById('admin-organizer').value = ev.organizer || '';
        document.getElementById('admin-topic').value = ev.topic || 'Other';
        document.getElementById('admin-type').value = ev.eventType || 'WORKSHOP';
        document.getElementById('admin-mode').value = ev.mode || 'ONLINE';
        document.getElementById('admin-city').value = ev.city || 'Online';
        document.getElementById('admin-venue').value = ev.venueOrPlatform === 'Not provided' ? '' : (ev.venueOrPlatform || '');
        document.getElementById('admin-start').value = toDatetimeLocalInput(ev.startTime);
        document.getElementById('admin-end').value = toDatetimeLocalInput(ev.endTime);
        document.getElementById('admin-deadline').value = toDatetimeLocalInput(ev.registrationDeadline);
        document.getElementById('admin-cost-type').value = ev.costType || 'NOT_PROVIDED';
        document.getElementById('admin-price').value = ev.price !== null && ev.price !== undefined ? ev.price : '';
        document.getElementById('admin-eligibility').value = ev.eligibility === 'Not provided' ? '' : (ev.eligibility || '');
        document.getElementById('admin-reg-url').value = ev.registrationUrl || '';
        document.getElementById('admin-cancelled').checked = Boolean(ev.cancelled);
        document.getElementById('admin-reg-closed').checked = Boolean(ev.registrationClosed);
        document.getElementById('admin-description').value = ev.description || '';
        document.getElementById('admin-form-heading').scrollIntoView({ behavior: 'smooth' });
    }

    function resetAdminEventForm() {
        document.getElementById('admin-event-form').reset();
        document.getElementById('admin-event-id').value = '';
        document.getElementById('admin-form-heading').textContent = 'Add New Technology Event';
        document.getElementById('admin-btn-cancel-edit').classList.add('hidden');
        document.getElementById('admin-form-error').classList.add('hidden');
    }

    async function loadAdminParticipationsTable() {
        const wrapper = document.getElementById('admin-participations-table-wrapper');
        if (!wrapper) return;
        try {
            const list = await apiFetch('/api/admin/participations');
            if (!list || list.length === 0) {
                wrapper.innerHTML = `<div class="empty-state-panel"><p>No student participation records submitted yet.</p></div>`;
                return;
            }
            wrapper.innerHTML = `
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Student</th>
                            <th>Event &amp; Type</th>
                            <th>Status</th>
                            <th>Outcome</th>
                            <th>Credits</th>
                            <th>Verification Flag</th>
                            <th>Faculty Note</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.map(p => `
                            <tr>
                                <td>
                                    <strong>${escapeHtml(p.userFullName)}</strong><br/>
                                    <small>${escapeHtml(p.userEmail)}</small>
                                </td>
                                <td>
                                    <strong>${escapeHtml(p.eventTitle)}</strong><br/>
                                    <small>${escapeHtml(p.eventType)}</small>
                                </td>
                                <td>
                                    <select id="adm-part-status-${p.id}" class="form-select">
                                        <option value="SAVED" ${p.status === 'SAVED' ? 'selected' : ''}>SAVED</option>
                                        <option value="REGISTERED" ${p.status === 'REGISTERED' ? 'selected' : ''}>REGISTERED</option>
                                        <option value="ATTENDED" ${p.status === 'ATTENDED' ? 'selected' : ''}>ATTENDED</option>
                                    </select>
                                </td>
                                <td>
                                    ${p.competitiveEvent ? `
                                        <select id="adm-part-outcome-${p.id}" class="form-select">
                                            <option value="PENDING" ${p.outcome === 'PENDING' ? 'selected' : ''}>PENDING</option>
                                            <option value="WON" ${p.outcome === 'WON' ? 'selected' : ''}>WON</option>
                                            <option value="NOT_WON" ${p.outcome === 'NOT_WON' ? 'selected' : ''}>NOT_WON</option>
                                        </select>
                                    ` : '<span class="tabular-nums">NONE</span>'}
                                </td>
                                <td class="tabular-nums"><strong>${p.credits}</strong></td>
                                <td>
                                    <label class="checkbox-label">
                                        <input type="checkbox" id="adm-part-verified-${p.id}" ${p.verifiedByAdmin ? 'checked' : ''} />
                                        <span>${p.verifiedByAdmin ? 'Verified ✓' : 'Self-Reported'}</span>
                                    </label>
                                </td>
                                <td>
                                    <input type="text" id="adm-part-note-${p.id}" class="form-input" value="${escapeHtml(p.adminVerificationNote || '')}" placeholder="Certificate / attendance ref" />
                                </td>
                                <td>
                                    <button type="button" class="btn btn-primary btn-sm js-admin-verify-btn" data-id="${p.id}" data-comp="${p.competitiveEvent}">
                                        Apply
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;

            wrapper.querySelectorAll('.js-admin-verify-btn').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const id = parseInt(btn.getAttribute('data-id'), 10);
                    const isComp = btn.getAttribute('data-comp') === 'true';
                    const status = document.getElementById(`adm-part-status-${id}`).value;
                    const outcomeEl = document.getElementById(`adm-part-outcome-${id}`);
                    const outcome = isComp && outcomeEl ? outcomeEl.value : 'NONE';
                    const verifiedByAdmin = document.getElementById(`adm-part-verified-${id}`).checked;
                    const adminVerificationNote = document.getElementById(`adm-part-note-${id}`).value;

                    try {
                        await apiFetch(`/api/admin/participations/${id}/verify`, {
                            method: 'PUT',
                            body: JSON.stringify({ status, outcome, verifiedByAdmin, adminVerificationNote })
                        });
                        showToast(`Updated & verified participation record #${id}.`);
                        loadAdminParticipationsTable();
                    } catch (err) {
                        showToast(err.message, true);
                    }
                });
            });
        } catch (err) {
            wrapper.innerHTML = `<div class="empty-state-panel"><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    // =========================================================================
    // 12. Event Listeners Setup on DOMContentLoaded
    // =========================================================================
    document.addEventListener('DOMContentLoaded', async () => {
        initTheme();

        const themeBtn = document.getElementById('theme-toggle-btn');
        if (themeBtn) themeBtn.addEventListener('click', toggleTheme);

        // Modal controls
        document.getElementById('confirm-modal-cancel').addEventListener('click', closeConfirmModal);
        document.getElementById('confirm-modal-proceed').addEventListener('click', async () => {
            const cb = state.confirmCallback;
            closeConfirmModal();
            if (typeof cb === 'function') await cb();
        });

        // Back button on Details
        document.getElementById('btn-back-to-explore').addEventListener('click', () => {
            window.location.hash = '#' + (state.lastActiveView || 'explore');
        });

        // Refresh Source buttons
        document.getElementById('btn-refresh-source').addEventListener('click', handleRefreshSource);
        const adminRefreshBtn = document.getElementById('admin-btn-refresh-source');
        if (adminRefreshBtn) adminRefreshBtn.addEventListener('click', handleRefreshSource);

        // Explore Filter Form
        const filterForm = document.getElementById('explore-filter-form');
        if (filterForm) {
            filterForm.addEventListener('submit', (e) => {
                e.preventDefault();
                state.currentPage = 0;
                loadExploreEvents();
            });
        }
        document.getElementById('btn-reset-filters').addEventListener('click', resetExploreFilters);

        // Demo Account Quick-Fill Buttons
        document.getElementById('btn-fill-student-demo').addEventListener('click', () => {
            document.getElementById('login-email').value = 'student@techpulse.edu.in';
            document.getElementById('login-password').value = 'Student@123';
            showToast('Filled Student demo credentials. Click Sign In.');
        });

        document.getElementById('btn-fill-admin-demo').addEventListener('click', () => {
            document.getElementById('login-email').value = 'admin@techpulse.edu.in';
            document.getElementById('login-password').value = 'Admin@123';
            showToast('Filled Admin demo credentials. Click Sign In.');
        });

        // Login Form Submit
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const errBox = document.getElementById('login-error-msg');
            errBox.classList.add('hidden');
            const email = document.getElementById('login-email').value.trim();
            const password = document.getElementById('login-password').value;

            try {
                const profile = await apiFetch('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({ email, password })
                });
                state.currentUser = profile;
                if (profile.csrfToken) state.csrfToken = profile.csrfToken;
                renderHeaderAuthControls();
                document.getElementById('login-form').reset();
                showToast(`Welcome back, ${profile.fullName}!`);
                window.location.hash = profile.role === 'ADMIN' ? '#admin' : '#explore';
            } catch (err) {
                errBox.textContent = err.message;
                errBox.classList.remove('hidden');
            }
        });

        // Register Form Submit
        document.getElementById('register-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const errBox = document.getElementById('register-error-msg');
            errBox.classList.add('hidden');
            const fullName = document.getElementById('reg-fullname').value.trim();
            const email = document.getElementById('reg-email').value.trim();
            const collegeOrInstitution = document.getElementById('reg-college').value.trim();
            const password = document.getElementById('reg-password').value;

            try {
                const profile = await apiFetch('/api/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({ fullName, email, collegeOrInstitution, password })
                });
                state.currentUser = profile;
                if (profile.csrfToken) state.csrfToken = profile.csrfToken;
                renderHeaderAuthControls();
                document.getElementById('register-form').reset();
                showToast(`Account created for ${profile.fullName}!`);
                window.location.hash = '#explore';
            } catch (err) {
                errBox.textContent = err.message;
                errBox.classList.remove('hidden');
            }
        });

        // Admin Event Form Submit
        document.getElementById('admin-btn-cancel-edit').addEventListener('click', resetAdminEventForm);
        document.getElementById('admin-event-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const errBox = document.getElementById('admin-form-error');
            errBox.classList.add('hidden');

            const editId = document.getElementById('admin-event-id').value.trim();
            const startTime = fromDatetimeLocalToIsoIst(document.getElementById('admin-start').value);
            const endTime = fromDatetimeLocalToIsoIst(document.getElementById('admin-end').value);
            const registrationDeadline = fromDatetimeLocalToIsoIst(document.getElementById('admin-deadline').value);

            // Frontend validation mirror
            if (!startTime) {
                errBox.textContent = 'Start date and time are required.';
                errBox.classList.remove('hidden');
                return;
            }
            if (endTime && new Date(endTime) < new Date(startTime)) {
                errBox.textContent = 'End time cannot be earlier than start time.';
                errBox.classList.remove('hidden');
                return;
            }
            if (registrationDeadline && new Date(registrationDeadline) > new Date(startTime)) {
                errBox.textContent = 'Registration deadline cannot be after the event start time.';
                errBox.classList.remove('hidden');
                return;
            }

            const costType = document.getElementById('admin-cost-type').value;
            const rawPrice = document.getElementById('admin-price').value.trim();
            const payload = {
                title: document.getElementById('admin-title').value.trim(),
                organizer: document.getElementById('admin-organizer').value.trim(),
                topic: document.getElementById('admin-topic').value,
                eventType: document.getElementById('admin-type').value,
                mode: document.getElementById('admin-mode').value,
                city: document.getElementById('admin-city').value.trim(),
                venueOrPlatform: document.getElementById('admin-venue').value.trim(),
                startTime,
                endTime,
                registrationDeadline,
                costType,
                price: rawPrice !== '' ? parseFloat(rawPrice) : null,
                currency: 'INR',
                eligibility: document.getElementById('admin-eligibility').value.trim(),
                registrationUrl: document.getElementById('admin-reg-url').value.trim() || null,
                officialSourceUrl: document.getElementById('admin-reg-url').value.trim() || null,
                cancelled: document.getElementById('admin-cancelled').checked,
                registrationClosed: document.getElementById('admin-reg-closed').checked,
                description: document.getElementById('admin-description').value.trim()
            };

            try {
                if (editId) {
                    await apiFetch(`/api/admin/events/${editId}`, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    showToast(`Updated event #${editId} successfully.`);
                } else {
                    await apiFetch('/api/admin/events', {
                        method: 'POST',
                        body: JSON.stringify(payload)
                    });
                    showToast('Created new event successfully.');
                }
                resetAdminEventForm();
                loadAdminEventsTable();
            } catch (err) {
                errBox.textContent = err.message;
                errBox.classList.remove('hidden');
            }
        });

        await loadCurrentUser();
        await loadSourceStatus();
        window.addEventListener('hashchange', handleRouteChange);
        handleRouteChange();
    });
})();
