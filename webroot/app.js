// ---------------------------------------------------------------- API helper

async function api(method, path, params) {
  let url = path;
  let opts = { method, credentials: 'include' };
  if (method === 'GET' && params) {
    const qs = new URLSearchParams(params).toString();
    url += (qs ? '?' + qs : '');
  } else if (params) {
    opts.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    opts.body = new URLSearchParams(params).toString();
  }
  const res = await fetch(url, opts);
  let data;
  try { data = await res.json(); } catch (e) { data = { ok: false, error: 'Unexpected server response.' }; }
  if (!data.ok) throw new Error(data.error || 'Something went wrong.');
  return data;
}

// -------------------------------------------------------------------- toast

let toastTimer = null;
function toast(msg, isError) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = isError ? 'error show' : 'show';
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 3200);
}

function money(n) {
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// ------------------------------------------------------- printable documents

const BANK_NAME = 'Yousuf National Bank';

function printableShell(docTitle, bodyHtml) {
  return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>${docTitle} — ${BANK_NAME}</title>
<style>
  @page { margin: 18mm 16mm; }
  * { box-sizing: border-box; }
  body { font-family: Georgia, 'Times New Roman', serif; color: #1c1c1e; margin: 0; padding: 28px 34px; }
  .doc-header { display:flex; align-items:center; justify-content:space-between; border-bottom: 3px double #0f2438; padding-bottom: 14px; margin-bottom: 22px; }
  .doc-brand { display:flex; align-items:center; gap: 12px; }
  .doc-crest { width:44px; height:44px; border-radius:50%; background:#0f2438; border:2px solid #b08d57; color:#d4b483; display:flex; align-items:center; justify-content:center; font-size:22px; font-weight:600; }
  .doc-brand-text h1 { margin:0; font-size:19px; letter-spacing:0.4px; }
  .doc-brand-text div { font-size:11px; color:#6b7280; font-family: 'Courier New', monospace; }
  .doc-title { text-align:right; }
  .doc-title h2 { margin:0; font-size:15px; text-transform:uppercase; letter-spacing:1.5px; color:#0f2438; }
  .doc-title div { font-size:11px; color:#6b7280; font-family:'Courier New', monospace; margin-top:2px; }
  .doc-grid { display:grid; grid-template-columns: 1fr 1fr; gap: 6px 24px; margin: 18px 0 22px; font-size: 13.5px; }
  .doc-grid .k { color:#6b7280; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; }
  .doc-grid .v { font-family:'Courier New', monospace; font-weight:600; }
  .doc-amount { text-align:center; margin: 26px 0; padding: 18px; border: 1px solid #d5dae1; border-radius: 6px; background:#fafaf7; }
  .doc-amount .label { font-size:11px; text-transform:uppercase; letter-spacing:1px; color:#6b7280; }
  .doc-amount .value { font-size:30px; font-weight:700; color:#0f2438; margin-top:6px; font-family:'Courier New', monospace; }
  table.doc-table { width:100%; border-collapse:collapse; margin-top:10px; font-size:12.5px; }
  table.doc-table th { text-align:left; border-bottom:2px solid #0f2438; padding:6px 8px; font-size:10.5px; text-transform:uppercase; letter-spacing:0.5px; color:#374151; }
  table.doc-table td { padding:6px 8px; border-bottom:1px solid #e5e9ef; font-family:'Courier New', monospace; }
  table.doc-table td.num, table.doc-table th.num { text-align:right; }
  .doc-footer { margin-top: 40px; display:flex; justify-content:space-between; align-items:flex-end; }
  .doc-sign { text-align:center; width: 220px; }
  .doc-sign .line { border-top:1px solid #1c1c1e; margin-bottom:6px; }
  .doc-sign .label { font-size:11px; color:#6b7280; }
  .doc-note { font-size:10.5px; color:#9aa5b1; margin-top:26px; text-align:center; font-style:italic; }
  .print-bar { text-align:center; margin-bottom: 20px; }
  .print-bar button { font-family: Inter, sans-serif; font-size:13px; font-weight:600; padding:9px 18px; border-radius:6px; border:1px solid #0f2438; background:#0f2438; color:#fff; cursor:pointer; }
  @media print { .print-bar { display:none; } body { padding: 0; } }
</style></head>
<body>
  <div class="print-bar"><button onclick="window.print()">🖨 Print / Save as PDF</button></div>
  <div class="doc-header">
    <div class="doc-brand">
      <div class="doc-crest">Y</div>
      <div class="doc-brand-text"><h1>${BANK_NAME}</h1><div>123 Ledger Avenue &middot; est. 2026</div></div>
    </div>
    <div class="doc-title"><h2>${docTitle}</h2><div>Generated ${new Date().toLocaleString()}</div></div>
  </div>
  ${bodyHtml}
  <div class="doc-note">This document was generated electronically by ${BANK_NAME} and is valid without a physical signature.</div>
</body></html>`;
}

function openPrintable(docTitle, bodyHtml) {
  const w = window.open('', '_blank');
  if (!w) { toast('Please allow pop-ups to view/print this document.', true); return; }
  w.document.open();
  w.document.write(printableShell(docTitle, bodyHtml));
  w.document.close();
}

function txTypeLabel(t) { return t.replace(/_/g, ' '); }

function openTransactionVoucher(accNo, ownerName, t) {
  const body = `
    <div class="doc-grid">
      <div><div class="k">Voucher No.</div><div class="v">${t.id}</div></div>
      <div><div class="k">Date &amp; Time</div><div class="v">${t.timestamp.replace('T', ' ').substring(0, 19)}</div></div>
      <div><div class="k">Account Holder</div><div class="v">${ownerName}</div></div>
      <div><div class="k">Account Number</div><div class="v">${accNo}</div></div>
      <div><div class="k">Transaction Type</div><div class="v">${txTypeLabel(t.type)}</div></div>
      <div><div class="k">Balance After</div><div class="v">$${money(t.balanceAfter)}</div></div>
    </div>
    <div class="doc-amount">
      <div class="label">Amount</div>
      <div class="value">$${money(t.amount)}</div>
    </div>
    <div class="doc-grid" style="grid-template-columns: 1fr;">
      <div><div class="k">Description</div><div class="v" style="font-weight:400;">${t.description || '—'}</div></div>
    </div>
    <div class="doc-footer">
      <div class="doc-sign"><div class="line"></div><div class="label">Account Holder Signature</div></div>
      <div class="doc-sign"><div class="line"></div><div class="label">Authorized Bank Signature</div></div>
    </div>`;
  openPrintable('Transaction Voucher', body);
}

function openAccountStatement(account, transactions, ownerName) {
  const rows = transactions.slice().reverse().map(t => `
    <tr>
      <td>${t.timestamp.replace('T', ' ').substring(0, 19)}</td>
      <td>${t.id}</td>
      <td>${txTypeLabel(t.type)}</td>
      <td>${t.description || ''}</td>
      <td class="num">$${money(t.amount)}</td>
      <td class="num">$${money(t.balanceAfter)}</td>
    </tr>`).join('');
  const body = `
    <div class="doc-grid">
      <div><div class="k">Account Holder</div><div class="v">${ownerName}</div></div>
      <div><div class="k">Account Number</div><div class="v">${account.accountNumber}</div></div>
      <div><div class="k">Account Type</div><div class="v">${labelType(account.type)}</div></div>
      <div><div class="k">Current Balance</div><div class="v">$${money(account.balance)}</div></div>
      <div><div class="k">Opened On</div><div class="v">${account.openedOn}</div></div>
      <div><div class="k">Statement Covers</div><div class="v">All ${transactions.length} recorded transactions</div></div>
    </div>
    ${transactions.length === 0 ? '<p style="color:#6b7280;">No transactions recorded on this account yet.</p>' : `
    <table class="doc-table">
      <thead><tr><th>Date</th><th>Voucher No.</th><th>Type</th><th>Description</th><th class="num">Amount</th><th class="num">Balance</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>`}
    <div class="doc-footer">
      <div class="doc-sign"><div class="line"></div><div class="label">Authorized Bank Signature</div></div>
      <div></div>
    </div>`;
  openPrintable('Account Statement', body);
}

function openPayslip(ps) {
  const body = `
    <div class="doc-grid">
      <div><div class="k">Voucher No.</div><div class="v">${ps.voucherId}</div></div>
      <div><div class="k">Pay Date</div><div class="v">${ps.payDate}</div></div>
      <div><div class="k">Employee</div><div class="v">${ps.employeeName}</div></div>
      <div><div class="k">Employee ID</div><div class="v">${ps.employeeId}</div></div>
      <div><div class="k">Employer</div><div class="v">${ps.employerName}</div></div>
      <div><div class="k">Position</div><div class="v">${ps.position}</div></div>
      <div><div class="k">Credited To</div><div class="v">${ps.accountNumber}</div></div>
      <div><div class="k">Balance After</div><div class="v">$${money(ps.newBalance)}</div></div>
    </div>
    <div class="doc-amount">
      <div class="label">Net Salary Paid</div>
      <div class="value">$${money(ps.amount)}</div>
    </div>
    <div class="doc-footer">
      <div class="doc-sign"><div class="line"></div><div class="label">Employee Signature</div></div>
      <div class="doc-sign"><div class="line"></div><div class="label">Employer / Authorized Signature</div></div>
    </div>`;
  openPrintable('Salary Voucher', body);
}

// ------------------------------------------------------------------- modals

function openModal(title, fieldsHtml, onSubmit) {
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.innerHTML = `
    <div class="modal">
      <h3>${title}</h3>
      <div class="modal-body">${fieldsHtml}</div>
      <div class="btn-row">
        <button class="btn btn-ghost btn-sm" data-action="cancel">Cancel</button>
        <button class="btn btn-brass btn-sm" data-action="submit">Confirm</button>
      </div>
    </div>`;
  document.body.appendChild(backdrop);

  backdrop.querySelector('[data-action="cancel"]').onclick = () => backdrop.remove();
  backdrop.addEventListener('click', (e) => { if (e.target === backdrop) backdrop.remove(); });
  backdrop.querySelector('[data-action="submit"]').onclick = async () => {
    try {
      await onSubmit(backdrop);
      backdrop.remove();
    } catch (e) {
      toast(e.message, true);
    }
  };
  return backdrop;
}

function field(label, id, type, extra) {
  return `<div class="field"><label>${label}</label><input id="${id}" type="${type || 'text'}" ${extra || ''}></div>`;
}

function selectField(label, id, options) {
  const opts = options.map(o => `<option value="${o.value}">${o.label}</option>`).join('');
  return `<div class="field"><label>${label}</label><select id="${id}">${opts}</select></div>`;
}

// =============================================================== LOGIN VIEW

document.querySelectorAll('.login-tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.login-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.login-pane').forEach(p => p.classList.remove('active'));
    tab.classList.add('active');
    document.getElementById('pane-' + tab.dataset.tab).classList.add('active');
  });
});

document.getElementById('btn-login').addEventListener('click', async () => {
  const statusEl = document.getElementById('login-status');
  statusEl.textContent = '';
  try {
    const data = await api('POST', '/api/login', {
      username: document.getElementById('login-username').value,
      pin: document.getElementById('login-pin').value
    });
    enterApp('CUSTOMER', document.getElementById('login-username').value, data.fullName);
  } catch (e) {
    statusEl.className = 'status-msg error';
    statusEl.textContent = e.message;
  }
});

document.getElementById('btn-register').addEventListener('click', async () => {
  const statusEl = document.getElementById('register-status');
  statusEl.textContent = '';
  try {
    await api('POST', '/api/register', {
      username: document.getElementById('reg-username').value,
      pin: document.getElementById('reg-pin').value,
      fullName: document.getElementById('reg-fullname').value,
      email: document.getElementById('reg-email').value,
      phone: document.getElementById('reg-phone').value
    });
    statusEl.className = 'status-msg success';
    statusEl.textContent = 'Account created! Switch to Sign In to continue.';
  } catch (e) {
    statusEl.className = 'status-msg error';
    statusEl.textContent = e.message;
  }
});

document.getElementById('btn-admin-login').addEventListener('click', async () => {
  const statusEl = document.getElementById('admin-status');
  statusEl.textContent = '';
  try {
    const data = await api('POST', '/api/admin-login', {
      username: document.getElementById('admin-username').value,
      password: document.getElementById('admin-password').value
    });
    enterApp('ADMIN', data.username, null);
  } catch (e) {
    statusEl.className = 'status-msg error';
    statusEl.textContent = e.message;
  }
});

document.getElementById('btn-logout').addEventListener('click', async () => {
  try { await api('POST', '/api/logout'); } catch (e) {}
  location.reload();
});

// ================================================================ APP SHELL

let currentRole = null;
let currentUsername = null;

function enterApp(role, username, fullName) {
  currentRole = role;
  currentUsername = username;
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('shell').classList.remove('hidden');
  document.getElementById('who-label').textContent = role === 'ADMIN' ? 'Bank Staff' : (fullName || username);

  if (role === 'ADMIN') {
    document.getElementById('nav-admin').classList.remove('hidden');
    document.getElementById('nav-customer').classList.add('hidden');
    showView('admin-customers');
    document.querySelectorAll('#nav-admin .nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === 'admin-customers'));
  } else {
    document.getElementById('nav-customer').classList.remove('hidden');
    document.getElementById('nav-admin').classList.add('hidden');
    showView('overview');
    document.querySelectorAll('#nav-customer .nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === 'overview'));
  }
}

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', () => {
    const container = item.closest('nav');
    container.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    item.classList.add('active');
    showView(item.dataset.view);
  });
});

function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
  document.getElementById('view-' + name).classList.remove('hidden');
  const loaders = {
    overview: loadOverview,
    accounts: loadAccounts,
    transactions: loadTransactionsView,
    loans: loadLoans,
    profile: loadProfile,
    'admin-customers': loadAdminCustomers,
    'admin-accounts': loadAdminAccounts,
    'admin-loans': loadAdminLoans,
    'admin-payroll': loadAdminPayroll,
    'admin-reports': loadAdminReports
  };
  if (loaders[name]) loaders[name]();
}

// -------------------------------------------------------------- restore session

(async function checkSession() {
  try {
    const data = await api('GET', '/api/me');
    if (data.loggedIn) enterApp(data.role, data.username, data.fullName);
  } catch (e) {}
})();

// =========================================================== CUSTOMER: OVERVIEW

async function loadOverview() {
  const me = await api('GET', '/api/me');
  document.getElementById('overview-greeting').textContent = 'Welcome back, ' + (me.fullName || me.username);

  const data = await api('GET', '/api/accounts');
  myAccountsCache = data.accounts;

  document.getElementById('ov-total-balance').textContent = '$' + money(data.totalBalance);
  document.getElementById('ov-account-count').textContent =
    data.accounts.length + (data.accounts.length === 1 ? ' account open' : ' accounts open');

  const cardsWrap = document.getElementById('ov-account-cards');
  if (data.accounts.length === 0) {
    cardsWrap.innerHTML = `<div class="empty-state">No accounts yet — use "Open Account" above to get started.</div>`;
  } else {
    cardsWrap.innerHTML = data.accounts.map(a => `
      <div class="tile">
        <div class="tile-label">${labelType(a.type)} &middot; ${a.accountNumber}</div>
        <div class="tile-value tabular">$${money(a.balance)}</div>
      </div>`).join('');
  }

  drawBalanceChart(data.accounts);

  // Pull recent transactions across every account, merge, and show the latest few.
  const recentWrap = document.getElementById('ov-recent-wrap');
  if (data.accounts.length === 0) {
    recentWrap.innerHTML = `<div class="empty-state">Nothing to show yet.</div>`;
    return;
  }
  const perAccount = await Promise.all(data.accounts.map(a =>
    api('GET', '/api/transactions', { accountNumber: a.accountNumber })
      .then(r => r.transactions.map(t => Object.assign({ accountNumber: a.accountNumber }, t)))
  ));
  const merged = [].concat(...perAccount).sort((a, b) => b.timestamp.localeCompare(a.timestamp)).slice(0, 6);
  if (merged.length === 0) {
    recentWrap.innerHTML = `<div class="empty-state">No transactions yet.</div>`;
    return;
  }
  const inflow = new Set(['DEPOSIT', 'TRANSFER_IN', 'INTEREST_CREDIT', 'LOAN_DISBURSEMENT', 'SALARY_CREDIT']);
  recentWrap.innerHTML = merged.map(t => `
    <div class="recent-item">
      <div>
        <div class="recent-desc">${txTypeLabel(t.type)} &middot; ${t.accountNumber}</div>
        <div class="recent-meta">${t.timestamp.replace('T', ' ').substring(0, 16)} &middot; ${t.description || ''}</div>
      </div>
      <div class="recent-amount ${inflow.has(t.type) ? 'positive' : 'negative'}">
        ${inflow.has(t.type) ? '+' : '−'}$${money(t.amount)}
      </div>
    </div>`).join('');
}

function drawBalanceChart(accounts) {
  const canvas = document.getElementById('ov-chart');
  const ctx = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  ctx.clearRect(0, 0, w, h);
  if (accounts.length === 0) return;

  const max = Math.max(...accounts.map(a => a.balance), 1);
  const barCount = accounts.length;
  const gap = 14;
  const barWidth = Math.min(46, (w - gap * (barCount + 1)) / barCount);
  const chartH = h - 28;
  let x = gap;

  ctx.font = '10px Inter, sans-serif';
  accounts.forEach(a => {
    const barH = Math.max(4, (a.balance / max) * chartH);
    const y = chartH - barH + 6;
    const grad = ctx.createLinearGradient(0, y, 0, y + barH);
    grad.addColorStop(0, '#d4b483');
    grad.addColorStop(1, '#b08d57');
    ctx.fillStyle = grad;
    ctx.fillRect(x, y, barWidth, barH);
    ctx.fillStyle = '#b9c6da';
    ctx.textAlign = 'center';
    ctx.fillText(a.accountNumber.replace('ACC', '#'), x + barWidth / 2, h - 8);
    x += barWidth + gap;
  });
}

document.getElementById('qa-deposit').addEventListener('click', () => document.getElementById('btn-deposit').click());
document.getElementById('qa-withdraw').addEventListener('click', () => document.getElementById('btn-withdraw').click());
document.getElementById('qa-transfer').addEventListener('click', () => document.getElementById('btn-transfer').click());
document.getElementById('qa-open').addEventListener('click', () => document.getElementById('btn-open-account').click());

// =========================================================== CUSTOMER: ACCOUNTS

let myAccountsCache = [];

async function loadAccounts() {
  const data = await api('GET', '/api/accounts');
  myAccountsCache = data.accounts;

  document.getElementById('accounts-tiles').innerHTML = `
    <div class="tile"><div class="tile-label">Total Balance</div><div class="tile-value tabular">$${money(data.totalBalance)}</div></div>
    <div class="tile"><div class="tile-label">Open Accounts</div><div class="tile-value tabular">${data.accounts.length}</div></div>
  `;

  const wrap = document.getElementById('accounts-table-wrap');
  if (data.accounts.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No accounts yet — open your first one above.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th>Account #</th><th>Type</th><th>Status</th><th style="text-align:right">Balance</th></tr></thead>
      <tbody>
        ${data.accounts.map(a => `
          <tr>
            <td class="tabular">${a.accountNumber}</td>
            <td>${labelType(a.type)}</td>
            <td><span class="badge ${a.frozen ? 'badge-frozen' : 'badge-active'}">${a.frozen ? 'Frozen' : 'Active'}</span></td>
            <td class="num">$${money(a.balance)}</td>
          </tr>`).join('')}
      </tbody>
    </table>`;
}

function labelType(t) {
  return { SAVINGS: 'Savings', CHECKING: 'Checking', 'FIXED-D': 'Fixed Deposit' }[t] || t;
}

document.getElementById('btn-open-account').addEventListener('click', () => {
  openModal('Open New Account',
    selectField('Account Type', 'm-type', [
      { value: 'SAVINGS', label: 'Savings — 3% interest, min. balance $100' },
      { value: 'CHECKING', label: 'Checking — $500 overdraft cushion' },
      { value: 'FIXED', label: 'Fixed Deposit — 7% interest, locked term' }
    ]) +
    field('Opening Deposit', 'm-amount', 'number') +
    field('Term in Months (Fixed Deposit only)', 'm-term', 'number', 'value="12"'),
    async (modal) => {
      await api('POST', '/api/accounts/open', {
        type: modal.querySelector('#m-type').value,
        amount: modal.querySelector('#m-amount').value,
        term: modal.querySelector('#m-term').value
      });
      toast('Account opened.');
      loadAccounts();
    });
});

document.getElementById('btn-deposit').addEventListener('click', () => {
  if (myAccountsCache.length === 0) { toast('Open an account first.', true); return; }
  openModal('Deposit', selectField('Account', 'm-acc', myAccountsCache.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) + field('Amount', 'm-amount', 'number'),
    async (modal) => {
      const data = await api('POST', '/api/accounts/deposit', { accountNumber: modal.querySelector('#m-acc').value, amount: modal.querySelector('#m-amount').value });
      toast('Deposited. New balance: $' + money(data.newBalance));
      loadAccounts();
    });
});

document.getElementById('btn-withdraw').addEventListener('click', () => {
  if (myAccountsCache.length === 0) { toast('Open an account first.', true); return; }
  openModal('Withdraw', selectField('Account', 'm-acc', myAccountsCache.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) + field('Amount', 'm-amount', 'number'),
    async (modal) => {
      const data = await api('POST', '/api/accounts/withdraw', { accountNumber: modal.querySelector('#m-acc').value, amount: modal.querySelector('#m-amount').value });
      toast('Withdrawn. New balance: $' + money(data.newBalance));
      loadAccounts();
    });
});

document.getElementById('btn-transfer').addEventListener('click', () => {
  if (myAccountsCache.length === 0) { toast('Open an account first.', true); return; }
  openModal('Transfer Funds',
    selectField('From Account', 'm-from', myAccountsCache.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
    field('Destination Account #', 'm-to') +
    field('Amount', 'm-amount', 'number') +
    field('Note (optional)', 'm-note'),
    async (modal) => {
      await api('POST', '/api/accounts/transfer', {
        fromAccount: modal.querySelector('#m-from').value,
        toAccount: modal.querySelector('#m-to').value,
        amount: modal.querySelector('#m-amount').value,
        note: modal.querySelector('#m-note').value
      });
      toast('Transfer complete.');
      loadAccounts();
    });
});

// ======================================================= CUSTOMER: TRANSACTIONS

async function loadTransactionsView() {
  const data = await api('GET', '/api/accounts');
  myAccountsCache = data.accounts;
  const picker = document.getElementById('tx-account-picker');
  const prev = picker.value;
  picker.innerHTML = data.accounts.map(a => `<option value="${a.accountNumber}">${a.accountNumber} — ${labelType(a.type)}</option>`).join('');
  if (data.accounts.length === 0) {
    document.getElementById('tx-table-wrap').innerHTML = `<div class="empty-state">No accounts yet.</div>`;
    return;
  }
  picker.value = prev && data.accounts.some(a => a.accountNumber === prev) ? prev : data.accounts[0].accountNumber;
  picker.onchange = loadTransactionsTable;
  loadTransactionsTable();
}

let txCache = [];

async function loadTransactionsTable() {
  const accNo = document.getElementById('tx-account-picker').value;
  if (!accNo) return;
  const data = await api('GET', '/api/transactions', { accountNumber: accNo });
  txCache = data.transactions;
  const wrap = document.getElementById('tx-table-wrap');
  if (data.transactions.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No transactions yet on this account.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th>Date</th><th>Type</th><th style="text-align:right">Amount</th><th style="text-align:right">Balance After</th><th>Description</th><th></th></tr></thead>
      <tbody>
        ${data.transactions.slice().reverse().map(t => `
          <tr>
            <td class="tabular">${t.timestamp.replace('T', ' ').substring(0, 19)}</td>
            <td>${t.type.replace(/_/g, ' ')}</td>
            <td class="num">$${money(t.amount)}</td>
            <td class="num">$${money(t.balanceAfter)}</td>
            <td>${t.description || ''}</td>
            <td><button class="btn btn-ghost btn-sm" data-receipt="${t.id}" style="padding:5px 10px;">Receipt</button></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('[data-receipt]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const t = txCache.find(x => x.id === btn.dataset.receipt);
      const me = await api('GET', '/api/me');
      openTransactionVoucher(accNo, me.fullName || me.username, t);
    });
  });
}

document.getElementById('btn-download-statement').addEventListener('click', async () => {
  const accNo = document.getElementById('tx-account-picker').value;
  if (!accNo) { toast('No account selected.', true); return; }
  const account = myAccountsCache.find(a => a.accountNumber === accNo);
  const me = await api('GET', '/api/me');
  openAccountStatement(account, txCache, me.fullName || me.username);
});

// ============================================================ CUSTOMER: LOANS

let myLoansCache = [];
let selectedLoanId = null;

async function loadLoans() {
  const accData = await api('GET', '/api/accounts');
  myAccountsCache = accData.accounts;
  const data = await api('GET', '/api/loans');
  myLoansCache = data.loans;
  const wrap = document.getElementById('loans-table-wrap');
  if (data.loans.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No loans yet.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>Loan ID</th><th style="text-align:right">Principal</th><th>Term</th><th style="text-align:right">Owed</th><th>Status</th></tr></thead>
      <tbody>
        ${data.loans.map(l => `
          <tr class="selectable ${l.loanId === selectedLoanId ? 'selected' : ''}" data-loan="${l.loanId}">
            <td><input type="radio" name="loanpick" ${l.loanId === selectedLoanId ? 'checked' : ''}></td>
            <td class="tabular">${l.loanId}</td>
            <td class="num">$${money(l.principal)}</td>
            <td>${l.termMonths} mo</td>
            <td class="num">$${money(l.outstandingBalance)}</td>
            <td><span class="badge badge-${l.status.toLowerCase()}">${l.status.replace('_', ' ')}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => {
      selectedLoanId = row.dataset.loan;
      loadLoans();
    });
  });
}

document.getElementById('btn-apply-loan').addEventListener('click', () => {
  if (myAccountsCache.length === 0) { toast('Open an account first to receive the funds.', true); return; }
  openModal('Apply For Loan',
    selectField('Disburse To', 'm-acc', myAccountsCache.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
    field('Loan Amount', 'm-amount', 'number') +
    field('Term (months)', 'm-term', 'number', 'value="12"'),
    async (modal) => {
      await api('POST', '/api/loans/apply', {
        accountNumber: modal.querySelector('#m-acc').value,
        amount: modal.querySelector('#m-amount').value,
        term: modal.querySelector('#m-term').value
      });
      toast('Application submitted — awaiting admin approval.');
      loadLoans();
    });
});

document.getElementById('btn-repay-loan').addEventListener('click', () => {
  if (!selectedLoanId) { toast('Select a loan first.', true); return; }
  openModal('Repay Loan ' + selectedLoanId,
    selectField('From Account', 'm-acc', myAccountsCache.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
    field('Amount', 'm-amount', 'number'),
    async (modal) => {
      await api('POST', '/api/loans/repay', {
        loanId: selectedLoanId,
        accountNumber: modal.querySelector('#m-acc').value,
        amount: modal.querySelector('#m-amount').value
      });
      toast('Repayment successful.');
      loadLoans();
    });
});

// =========================================================== CUSTOMER: PROFILE

async function loadProfile() {
  const data = await api('GET', '/api/me');
  document.getElementById('profile-fullname').value = data.fullName || '';
  document.getElementById('profile-email').value = data.email || '';
  document.getElementById('profile-phone').value = data.phone || '';
}

document.getElementById('btn-save-profile').addEventListener('click', async () => {
  try {
    await api('POST', '/api/profile/update', {
      fullName: document.getElementById('profile-fullname').value,
      email: document.getElementById('profile-email').value,
      phone: document.getElementById('profile-phone').value
    });
    toast('Profile updated.');
  } catch (e) {
    toast(e.message, true);
  }
});

document.getElementById('btn-change-pin').addEventListener('click', async () => {
  const pin = document.getElementById('profile-newpin').value;
  if (!pin) { toast('Enter a new PIN first.', true); return; }
  try {
    await api('POST', '/api/profile/change-pin', { newPin: pin });
    document.getElementById('profile-newpin').value = '';
    toast('PIN updated.');
  } catch (e) {
    toast(e.message, true);
  }
});

// ================================================================= ADMIN

let selectedCustomerUsername = null;
let selectedAccountNumber = null;
let selectedAdminLoanId = null;

async function loadAdminCustomers() {
  const data = await api('GET', '/api/admin/customers');
  const wrap = document.getElementById('admin-customers-table-wrap');
  if (data.customers.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No customers yet.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>Username</th><th>Full Name</th><th>Email</th><th>Phone</th><th>Status</th></tr></thead>
      <tbody>
        ${data.customers.map(c => `
          <tr class="selectable ${c.username === selectedCustomerUsername ? 'selected' : ''}" data-user="${c.username}">
            <td><input type="radio" name="custpick" ${c.username === selectedCustomerUsername ? 'checked' : ''}></td>
            <td class="tabular">${c.username}</td>
            <td>${c.fullName}</td>
            <td>${c.email}</td>
            <td>${c.phone}</td>
            <td><span class="badge ${c.locked ? 'badge-locked' : 'badge-active'}">${c.locked ? 'Locked' : 'Active'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedCustomerUsername = row.dataset.user; loadAdminCustomers(); });
  });
}

document.getElementById('btn-toggle-lock').addEventListener('click', async () => {
  if (!selectedCustomerUsername) { toast('Select a customer first.', true); return; }
  try {
    await api('POST', '/api/admin/customers/toggle-lock', { username: selectedCustomerUsername });
    toast('Customer status updated.');
    loadAdminCustomers();
  } catch (e) { toast(e.message, true); }
});

async function loadAdminAccounts() {
  const data = await api('GET', '/api/admin/accounts');
  const wrap = document.getElementById('admin-accounts-table-wrap');
  if (data.accounts.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No accounts yet.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>Account #</th><th>Owner</th><th>Type</th><th style="text-align:right">Balance</th><th>Status</th></tr></thead>
      <tbody>
        ${data.accounts.map(a => `
          <tr class="selectable ${a.accountNumber === selectedAccountNumber ? 'selected' : ''}" data-acc="${a.accountNumber}">
            <td><input type="radio" name="accpick" ${a.accountNumber === selectedAccountNumber ? 'checked' : ''}></td>
            <td class="tabular">${a.accountNumber}</td>
            <td>${a.owner}</td>
            <td>${labelType(a.type)}</td>
            <td class="num">$${money(a.balance)}</td>
            <td><span class="badge ${a.frozen ? 'badge-frozen' : 'badge-active'}">${a.frozen ? 'Frozen' : 'Active'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedAccountNumber = row.dataset.acc; loadAdminAccounts(); });
  });
}

document.getElementById('btn-toggle-freeze').addEventListener('click', async () => {
  if (!selectedAccountNumber) { toast('Select an account first.', true); return; }
  try {
    await api('POST', '/api/admin/accounts/toggle-freeze', { accountNumber: selectedAccountNumber });
    toast('Account status updated.');
    loadAdminAccounts();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-apply-interest').addEventListener('click', async () => {
  try {
    await api('POST', '/api/admin/accounts/apply-interest');
    toast('Interest applied to all eligible accounts.');
    loadAdminAccounts();
  } catch (e) { toast(e.message, true); }
});

async function loadAdminLoans() {
  const data = await api('GET', '/api/admin/loans');
  const wrap = document.getElementById('admin-loans-table-wrap');
  if (data.loans.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No loans yet.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>Loan ID</th><th>Customer</th><th style="text-align:right">Principal</th><th>Term</th><th style="text-align:right">Owed</th><th>Status</th></tr></thead>
      <tbody>
        ${data.loans.map(l => `
          <tr class="selectable ${l.loanId === selectedAdminLoanId ? 'selected' : ''}" data-loan="${l.loanId}">
            <td><input type="radio" name="adminloanpick" ${l.loanId === selectedAdminLoanId ? 'checked' : ''}></td>
            <td class="tabular">${l.loanId}</td>
            <td>${l.customer}</td>
            <td class="num">$${money(l.principal)}</td>
            <td>${l.termMonths} mo</td>
            <td class="num">$${money(l.outstandingBalance)}</td>
            <td><span class="badge badge-${l.status.toLowerCase()}">${l.status.replace('_', ' ')}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedAdminLoanId = row.dataset.loan; loadAdminLoans(); });
  });
}

document.getElementById('btn-approve-loan').addEventListener('click', async () => {
  if (!selectedAdminLoanId) { toast('Select a loan first.', true); return; }
  try {
    await api('POST', '/api/admin/loans/approve', { loanId: selectedAdminLoanId });
    toast('Loan approved and disbursed.');
    loadAdminLoans();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-reject-loan').addEventListener('click', async () => {
  if (!selectedAdminLoanId) { toast('Select a loan first.', true); return; }
  try {
    await api('POST', '/api/admin/loans/reject', { loanId: selectedAdminLoanId });
    toast('Loan rejected.');
    loadAdminLoans();
  } catch (e) { toast(e.message, true); }
});

async function loadAdminReports() {
  const data = await api('GET', '/api/admin/summary');
  document.getElementById('admin-report-tiles').innerHTML = `
    <div class="tile"><div class="tile-label">Total Customers</div><div class="tile-value tabular">${data.totalCustomers}</div></div>
    <div class="tile"><div class="tile-label">Total Accounts</div><div class="tile-value tabular">${data.totalAccounts}</div></div>
    <div class="tile"><div class="tile-label">Total Deposits</div><div class="tile-value tabular">$${money(data.totalDeposits)}</div></div>
    <div class="tile"><div class="tile-label">Loans Issued</div><div class="tile-value tabular">${data.totalLoans}</div></div>
    <div class="tile"><div class="tile-label">Outstanding Loan Balance</div><div class="tile-value tabular">$${money(data.totalOutstandingLoans)}</div></div>
  `;
}

// ============================================================ ADMIN: PAYROLL

let selectedEmployeeId = null;

async function loadAdminPayroll() {
  const data = await api('GET', '/api/admin/payroll/employees');
  const wrap = document.getElementById('admin-payroll-table-wrap');
  if (data.employees.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No employees registered yet — add one above.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>ID</th><th>Employee</th><th>Account</th><th>Employer</th><th>Position</th><th style="text-align:right">Salary</th><th>Last Paid</th><th>Status</th></tr></thead>
      <tbody>
        ${data.employees.map(e => `
          <tr class="selectable ${e.employeeId === selectedEmployeeId ? 'selected' : ''}" data-emp="${e.employeeId}">
            <td><input type="radio" name="emppick" ${e.employeeId === selectedEmployeeId ? 'checked' : ''}></td>
            <td class="tabular">${e.employeeId}</td>
            <td>${e.employeeName}</td>
            <td class="tabular">${e.accountNumber}</td>
            <td>${e.employerName}</td>
            <td>${e.position}</td>
            <td class="num">$${money(e.monthlySalary)}</td>
            <td>${e.lastPaidOn || '—'}</td>
            <td><span class="badge ${e.active ? 'badge-active' : 'badge-frozen'}">${e.active ? 'Active' : 'Inactive'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedEmployeeId = row.dataset.emp; loadAdminPayroll(); });
  });
}

document.getElementById('btn-add-employee').addEventListener('click', async () => {
  try {
    await api('POST', '/api/admin/payroll/employees/add', {
      accountNumber: document.getElementById('pr-account').value,
      employerName: document.getElementById('pr-employer').value,
      position: document.getElementById('pr-position').value,
      monthlySalary: document.getElementById('pr-salary').value
    });
    toast('Employee added to payroll.');
    document.getElementById('pr-account').value = '';
    document.getElementById('pr-employer').value = '';
    document.getElementById('pr-position').value = '';
    document.getElementById('pr-salary').value = '';
    loadAdminPayroll();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-toggle-employee').addEventListener('click', async () => {
  if (!selectedEmployeeId) { toast('Select an employee first.', true); return; }
  try {
    await api('POST', '/api/admin/payroll/employees/toggle', { employeeId: selectedEmployeeId });
    toast('Employee status updated.');
    loadAdminPayroll();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-remove-employee').addEventListener('click', async () => {
  if (!selectedEmployeeId) { toast('Select an employee first.', true); return; }
  try {
    await api('POST', '/api/admin/payroll/employees/remove', { employeeId: selectedEmployeeId });
    toast('Employee removed from payroll.');
    selectedEmployeeId = null;
    loadAdminPayroll();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-run-payroll').addEventListener('click', async () => {
  try {
    const data = await api('POST', '/api/admin/payroll/run');
    if (data.count === 0) {
      toast('No active employees to pay (or all their accounts are frozen).', true);
      return;
    }
    toast(`Payroll complete — ${data.count} employee(s) paid.`);
    const card = document.getElementById('payroll-results-card');
    card.classList.remove('hidden');
    document.getElementById('payroll-results-wrap').innerHTML = `
      <table class="ledger">
        <thead><tr><th>Voucher</th><th>Employee</th><th>Employer</th><th style="text-align:right">Amount</th><th></th></tr></thead>
        <tbody>
          ${data.payslips.map(ps => `
            <tr>
              <td class="tabular">${ps.voucherId}</td>
              <td>${ps.employeeName}</td>
              <td>${ps.employerName}</td>
              <td class="num">$${money(ps.amount)}</td>
              <td><button class="btn btn-ghost btn-sm" data-payslip="${ps.voucherId}" style="padding:5px 10px;">Print Payslip</button></td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    document.getElementById('payroll-results-wrap').querySelectorAll('[data-payslip]').forEach(btn => {
      btn.addEventListener('click', () => {
        const ps = data.payslips.find(x => x.voucherId === btn.dataset.payslip);
        openPayslip(ps);
      });
    });
    loadAdminPayroll();
  } catch (e) { toast(e.message, true); }
});
