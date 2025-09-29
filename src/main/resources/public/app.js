const api = path => fetch(path, { headers: { "Content-Type": "application/json" } });

const el = (html) => {
    const t = document.createElement('template');
    t.innerHTML = html.trim();
    return t.content.firstChild;
};

// Simple router
function showView(hash) {
    const h = (hash || '#home').split('?')[0];
    const views = ['home','mocks','create'];
    views.forEach(v => {
        const elV = document.getElementById(`view-${v}`);
        if (elV) elV.style.display = (h === `#${v}`) ? '' : 'none';
        const nav = document.getElementById(`nav-${v}`);
        if (nav) nav.classList.toggle('active', h === `#${v}`);
    });
    if (h === '#mocks') refresh();
}

async function refresh() {
    setStatus("Loading mappings…");
    const res = await api('/api/mappings');
    const data = await res.json(); // { mappings: [ ... ] }
    renderList(data.mappings || []);
    setStatus(`Loaded ${ (data.mappings || []).length } mappings`);
}

function reqSummary(m) {
    const r = m.request || {};
    const method = r.method || '(any)';
    const url = r.url || (r.urlPattern ? `regex:${r.urlPattern}` : '(any)');
    return { method, url };
}

function renderList(mappings) {
    const list = document.getElementById('list');
    if (!list) return;
    list.innerHTML = '';

    if (!mappings.length) {
        list.appendChild(el(`<p class="muted">No mappings found.</p>`));
        return;
    }

    // Build table header
    const table = el(`
      <table class="mocks">
        <thead>
          <tr>
            <th style="width:25%">Name</th>
            <th style="width:30%">API</th>
            <th style="width:10%">HTTP Method</th>
            <th style="width:15%">Request</th>
            <th style="width:15%">Response</th>
            <th style="width:5%">Actions</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>`);
    const tbody = table.querySelector('tbody');

    for (const m of mappings) {
        const { method, url } = reqSummary(m);
        const req = JSON.stringify(m.request || {}, null, 2);
        const resp = JSON.stringify(m.response || {}, null, 2);
        const id = m.id;
        const name = m.name || `${method} ${url}`;

        const row = el(`
          <tr data-id="${id}">
            <td title="${escapeHtml(name)}">${escapeHtml(name)}</td>
            <td title="${escapeHtml(url)}" style="white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width: 60ch">${escapeHtml(url)}</td>
            <td><span class="tag">${method}</span></td>
            <td>
              <details>
                <summary><span class="muted">Show Request</span></summary>
                <pre style="margin-top:8px">${escapeHtml(req)}</pre>
              </details>
            </td>
            <td>
              <details>
                <summary><span class="muted">Show / Edit Response</span></summary>
                <div class="row" style="margin-top:8px">
                  <button class="btn" onclick="toggleRespEdit('${id}')">Toggle Edit</button>
                </div>
                <pre data-pre="${id}" style="margin-top:8px">${escapeHtml(resp)}</pre>
                <textarea data-id="${id}" class="resp" style="margin-top:8px; display:none">${resp}</textarea>
              </details>
            </td>
            <td>
              <div class="row">
                <button class="btn" onclick="updateMapping('${id}')">Edit / Save</button>
                <button class="btn danger" onclick="deleteMapping('${id}')">Delete</button>
              </div>
            </td>
          </tr>`);
        tbody.appendChild(row);
    }

    list.appendChild(table);
}

function toggleAcc(id){
    const acc = document.querySelector(`.accordion[data-acc-id="${id}"]`);
    if (!acc) return;
    const body = acc.querySelector('.acc-body');
    const isOpen = body.style.display === 'block';
    body.style.display = isOpen ? 'none' : 'block';
}

function toggleRespEdit(id){
    const ta = document.querySelector(`textarea[data-id="${id}"]`);
    const pre = document.querySelector(`pre[data-pre="${id}"]`);
    if (!ta || !pre) return;
    const taShown = ta.style.display !== 'none';
    if (taShown) {
        // Switching to pretty view: try to pretty-print current textarea JSON
        try {
            const obj = JSON.parse(ta.value);
            pre.textContent = JSON.stringify(obj, null, 2);
        } catch (_) {
            pre.textContent = ta.value; // fallback
        }
        ta.style.display = 'none';
        pre.style.display = 'block';
    } else {
        ta.style.display = 'block';
        pre.style.display = 'none';
        // Ensure textarea contains pretty JSON if possible
        try {
            const obj = JSON.parse(pre.textContent);
            ta.value = JSON.stringify(obj, null, 2);
        } catch (_) { /* leave as-is */ }
    }
}

function toggleSidebar(){
    const app = document.getElementById('appRoot');
    if (!app) return;
    const collapsed = !app.classList.contains('collapsed');
    app.classList.toggle('collapsed', collapsed);
    try { localStorage.setItem('sidebarCollapsed', collapsed ? '1' : '0'); } catch(_){}
}

async function updateMapping(id) {
    try {
        const ta = document.querySelector(`textarea[data-id="${id}"]`);
        const newResp = JSON.parse(ta.value);

        // Fetch full mapping, mutate response, PUT back
        const current = await (await api(`/api/mappings/${id}`)).json();
        current.response = newResp;

        const res = await fetch(`/api/mappings/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(current)
        });
        if (!res.ok) throw new Error(await res.text());
        setStatus(`Updated ${id}`);
    } catch (e) {
        setStatus(`Update failed: ${e.message}`);
    }
}

async function deleteMapping(id) {
    if (!confirm(`Delete mapping ${id}?`)) return;
    const res = await fetch(`/api/mappings/${id}`, { method: 'DELETE' });
    if (!res.ok) {
        setStatus(`Delete failed: ${await res.text()}`);
        return;
    }
    setStatus(`Deleted ${id}`);
    refresh();
}

async function createMapping() {
    try {
        const name = (document.getElementById('newName').value || '').trim();
        const method = (document.getElementById('newMethod').value || 'GET').toUpperCase();
        const url = document.getElementById('newUrl').value || '/';
        const status = parseInt(document.getElementById('newStatus').value || '200', 10);
        const headers = JSON.parse(document.getElementById('newHeaders').value || '{}');
        const bodyText = document.getElementById('newBody').value || '';

        const payload = {
            name: name || undefined,
            request: {
                method,
                url: url.startsWith('regex:') ? undefined : url,
                urlPattern: url.startsWith('regex:') ? url.replace(/^regex:/, '') : undefined
            },
            response: {
                status,
                headers,
                body: bodyText
            }
        };

        const res = await fetch('/api/mappings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error(await res.text());
        setStatus('Created mapping');
        location.hash = '#mocks';
        refresh();
    } catch (e) {
        setStatus(`Create failed: ${e.message}`);
    }
}

async function saveToFiles() {
    await fetch('/api/mappings/save', { method: 'POST' }).catch(()=>{});
    setStatus('Saved to files (mappings folder).');
}

async function resetAll() {
    if (!confirm('This will clear in-memory mappings. Continue?')) return;
    const res = await fetch('/api/mappings/reset', { method: 'POST' });
    if (!res.ok) {
        setStatus(`Reset failed: ${await res.text()}`);
        return;
    }
    setStatus('Reset complete.');
    refresh();
}

function setStatus(s) { const st = document.getElementById('status'); if (st) st.innerText = s; }
function escapeHtml(s) { return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;','\'':'&#39;'}[c])); }

window.addEventListener('hashchange', () => showView(location.hash || '#home'));
// Initialize sidebar state from localStorage
(function(){
  try {
    const collapsed = localStorage.getItem('sidebarCollapsed') === '1';
    const app = document.getElementById('appRoot');
    if (app && collapsed) app.classList.add('collapsed');
  } catch(_){}
})();
showView(location.hash || '#home');