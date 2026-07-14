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
  .doc-amount .doc-words { font-size:12px; color:#374151; margin-top:10px; font-style:italic; border-top:1px dashed #d5dae1; padding-top:10px; }
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

// -------------------------------------------------- amount-in-words (real bank
// vouchers always spell this out so the figure can't be silently altered)

function amountInWords(n) {
  const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
    'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
  const tens = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

  function chunk(num) {
    let s = '';
    if (num >= 100) { s += ones[Math.floor(num / 100)] + ' Hundred '; num %= 100; }
    if (num >= 20) { s += tens[Math.floor(num / 10)] + ' '; num %= 10; }
    if (num > 0) { s += ones[num] + ' '; }
    return s;
  }

  const whole = Math.floor(n);
  const cents = Math.round((n - whole) * 100);

  if (whole === 0) return 'Zero Dollars' + (cents > 0 ? ' and ' + chunk(cents).trim() + (cents === 1 ? ' Cent' : ' Cents') : '') + ' Only';

  const groups = [
    [1000000000, 'Billion'], [1000000, 'Million'], [1000, 'Thousand'], [1, '']
  ];
  let remaining = whole, words = '';
  for (const [value, label] of groups) {
    if (remaining >= value) {
      const count = Math.floor(remaining / value);
      words += chunk(count) + label + ' ';
      remaining %= value;
    }
  }
  words = words.trim() + (whole === 1 ? ' Dollar' : ' Dollars');
  if (cents > 0) words += ' and ' + chunk(cents).trim() + (cents === 1 ? ' Cent' : ' Cents');
  return words + ' Only';
}

function openTransactionVoucher(accNo, ownerName, t) {
  // Withdrawals are cash leaving the bank at the counter; transfers move money digitally
  // between two accounts; deposits are cash coming in; salary is a payroll credit. Each
  // is a different real-world event, so each gets the document that actually matches it.
  const docType = {
    WITHDRAWAL: { title: 'Cash Withdrawal Slip', kind: 'cash-out' },
    DEPOSIT: { title: 'Cash Deposit Slip', kind: 'cash-in' },
    TRANSFER_OUT: { title: 'Fund Transfer Advice (Outgoing)', kind: 'transfer' },
    TRANSFER_IN: { title: 'Fund Transfer Advice (Incoming)', kind: 'transfer' },
    SALARY_CREDIT: { title: 'Salary Credit Advice', kind: 'salary' },
    SALARY_DEBIT: { title: 'Payroll Disbursement Advice', kind: 'salary' },
    INTEREST_CREDIT: { title: 'Interest Credit Advice', kind: 'credit' },
    LOAN_DISBURSEMENT: { title: 'Loan Disbursement Voucher', kind: 'credit' },
    LOAN_REPAYMENT: { title: 'Loan Repayment Receipt', kind: 'cash-out' },
    FEE: { title: 'Fee Debit Advice', kind: 'cash-out' },
  }[t.type] || { title: 'Transaction Voucher', kind: 'other' };

  let counterpartyRow = '';
  const m = (t.description || '').match(/(?:to|from|To|From)\s+(ACC\d+)/);
  if (m && docType.kind === 'transfer') {
    counterpartyRow = `<div><div class="k">${t.type === 'TRANSFER_OUT' ? 'To Account' : 'From Account'}</div><div class="v">${m[1]}</div></div>`;
  }

  const signatureRow = docType.kind === 'cash-out'
    ? `<div class="doc-sign"><div class="line"></div><div class="label">Received in Cash By (Signature)</div></div>
       <div class="doc-sign"><div class="line"></div><div class="label">Paid By — Teller Signature</div></div>`
    : docType.kind === 'cash-in'
    ? `<div class="doc-sign"><div class="line"></div><div class="label">Deposited By (Signature)</div></div>
       <div class="doc-sign"><div class="line"></div><div class="label">Received By — Teller Signature</div></div>`
    : `<div class="doc-sign"><div class="line"></div><div class="label">Account Holder Signature</div></div>
       <div class="doc-sign"><div class="line"></div><div class="label">Authorized Bank Signature</div></div>`;

  const body = `
    <div class="doc-grid">
      <div><div class="k">Voucher No.</div><div class="v">${t.id}</div></div>
      <div><div class="k">Date &amp; Time</div><div class="v">${t.timestamp.replace('T', ' ').substring(0, 19)}</div></div>
      <div><div class="k">Account Holder</div><div class="v">${ownerName}</div></div>
      <div><div class="k">Account Number</div><div class="v">${accNo}</div></div>
      ${counterpartyRow}
      <div><div class="k">Balance After</div><div class="v">$${money(t.balanceAfter)}</div></div>
    </div>
    <div class="doc-amount">
      <div class="label">${docType.kind === 'cash-out' ? 'Amount Disbursed' : docType.kind === 'cash-in' ? 'Amount Received' : 'Amount'}</div>
      <div class="value">$${money(t.amount)}</div>
      <div class="doc-words">${amountInWords(t.amount)}</div>
    </div>
    <div class="doc-grid" style="grid-template-columns: 1fr;">
      <div><div class="k">Description</div><div class="v" style="font-weight:400;">${t.description || '—'}</div></div>
    </div>
    <div class="doc-footer">
      ${signatureRow}
    </div>`;
  openPrintable(docType.title, body);
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
      <div class="doc-words">${amountInWords(ps.amount)}</div>
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

document.getElementById('btn-staff-login').addEventListener('click', async () => {
  const statusEl = document.getElementById('staff-status');
  statusEl.textContent = '';
  try {
    const data = await api('POST', '/api/admin-login', {
      username: document.getElementById('staff-username').value,
      password: document.getElementById('staff-password').value
    });
    if (data.staffRole !== 'TELLER') {
      await api('POST', '/api/logout');
      statusEl.className = 'status-msg error';
      statusEl.textContent = 'That is an Admin account — please use the Admin Login tab instead.';
      return;
    }
    enterApp('ADMIN', data.username, null, data.staffRole);
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
    if (data.staffRole !== 'ADMIN') {
      await api('POST', '/api/logout');
      statusEl.className = 'status-msg error';
      statusEl.textContent = 'That is a Staff (Teller) account — please use the Staff Login tab instead.';
      return;
    }
    enterApp('ADMIN', data.username, null, data.staffRole);
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
let currentStaffRole = null;

function enterApp(role, username, fullName, staffRole) {
  currentRole = role;
  currentUsername = username;
  currentStaffRole = staffRole || null;
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('shell').classList.remove('hidden');

  if (role === 'ADMIN') {
    const roleLabel = currentStaffRole === 'ADMIN' ? 'Admin' : 'Teller';
    document.getElementById('who-label').textContent = roleLabel + ' — ' + username;
    document.getElementById('nav-admin').classList.remove('hidden');
    document.getElementById('nav-customer').classList.add('hidden');
    document.querySelectorAll('#nav-admin .admin-only').forEach(el => {
      el.classList.toggle('hidden', currentStaffRole !== 'ADMIN');
    });
    showView('admin-overview');
    document.querySelectorAll('#nav-admin .nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === 'admin-overview'));
  } else {
    document.getElementById('who-label').textContent = fullName || username;
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
    beneficiaries: loadBeneficiaries,
    'standing-orders': loadStandingOrders,
    profile: loadProfile,
    'admin-overview': loadAdminOverview,
    'admin-customers': loadAdminCustomers,
    'admin-accounts': loadAdminAccounts,
    'admin-loans': loadAdminLoans,
    'admin-payroll': loadAdminPayroll,
    'admin-staff': loadAdminStaff,
    'admin-audit': loadAdminAuditLog,
    'admin-reports': loadAdminReports
  };
  if (loaders[name]) loaders[name]();
}

// -------------------------------------------------------------- restore session

(async function checkSession() {
  try {
    const data = await api('GET', '/api/me');
    if (data.loggedIn) enterApp(data.role, data.username, data.fullName, data.staffRole);
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

  // Loan summary
  const loanData = await api('GET', '/api/loans');
  const activeLoans = loanData.loans.filter(l => l.status === 'APPROVED');
  const pendingLoans = loanData.loans.filter(l => l.status === 'PENDING');
  const owed = activeLoans.reduce((sum, l) => sum + l.outstandingBalance, 0);
  document.getElementById('ov-summary-tiles').innerHTML = `
    <div class="tile"><div class="tile-label">Active Loans</div><div class="tile-value tabular">${activeLoans.length}</div></div>
    <div class="tile"><div class="tile-label">Total Owed</div><div class="tile-value tabular">$${money(owed)}</div></div>
    <div class="tile"><div class="tile-label">Pending Applications</div><div class="tile-value tabular">${pendingLoans.length}</div></div>
  `;

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
document.getElementById('qa-loan').addEventListener('click', () => {
  jumpCustomerView('loans');
  document.getElementById('btn-apply-loan').click();
});
document.getElementById('qa-statement').addEventListener('click', () => {
  jumpCustomerView('transactions');
  toast('Choose an account, then click "Download Statement".');
});

function jumpCustomerView(view) {
  document.querySelectorAll('#nav-customer .nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === view));
  showView(view);
}

// =========================================================== CUSTOMER: ACCOUNTS

let myAccountsCache = [];

/** Accounts still usable for deposits/withdrawals/transfers/loans — closed accounts are excluded. */
function openAccountsCache() {
  return myAccountsCache.filter(a => !a.closed);
}

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
            <td><span class="badge ${a.closed ? 'badge-frozen' : a.frozen ? 'badge-pending' : 'badge-active'}">${a.closed ? 'Closed' : a.frozen ? 'Frozen' : 'Active'}</span></td>
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
  openModal('Deposit', selectField('Account', 'm-acc', openAccountsCache().map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) + field('Amount', 'm-amount', 'number'),
    async (modal) => {
      const data = await api('POST', '/api/accounts/deposit', { accountNumber: modal.querySelector('#m-acc').value, amount: modal.querySelector('#m-amount').value });
      toast('Deposited. New balance: $' + money(data.newBalance));
      loadAccounts();
    });
});

document.getElementById('btn-withdraw').addEventListener('click', () => {
  if (myAccountsCache.length === 0) { toast('Open an account first.', true); return; }
  openModal('Withdraw', selectField('Account', 'm-acc', openAccountsCache().map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) + field('Amount', 'm-amount', 'number'),
    async (modal) => {
      const data = await api('POST', '/api/accounts/withdraw', { accountNumber: modal.querySelector('#m-acc').value, amount: modal.querySelector('#m-amount').value });
      toast('Withdrawn. New balance: $' + money(data.newBalance));
      loadAccounts();
    });
});

document.getElementById('btn-transfer').addEventListener('click', async () => {
  if (myAccountsCache.length === 0) { toast('Open an account first.', true); return; }
  let benOptionsHtml = '<option value="">— Type a new account number below —</option>';
  try {
    const benData = await api('GET', '/api/beneficiaries');
    benOptionsHtml += benData.beneficiaries.map(b => `<option value="${b.accountNumber}">${b.nickname} (${b.accountNumber})</option>`).join('');
  } catch (e) { /* beneficiaries are optional; ignore failures here */ }

  const modal = openModal('Transfer Funds',
    selectField('From Account', 'm-from', openAccountsCache().map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
    `<div class="field"><label>Saved Payee (optional)</label><select id="m-payee">${benOptionsHtml}</select></div>` +
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
  modal.querySelector('#m-payee').addEventListener('change', (e) => {
    if (e.target.value) modal.querySelector('#m-to').value = e.target.value;
  });
});

document.getElementById('btn-close-account').addEventListener('click', () => {
  const eligible = openAccountsCache();
  if (eligible.length === 0) { toast('No open accounts to close.', true); return; }
  openModal('Close Account',
    selectField('Account to Close', 'm-acc', eligible.map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) + ' — $' + money(a.balance) }))) +
    '<p style="font-size:12.5px; color:var(--slate); margin-top:8px;">The account must have a zero balance. Withdraw or transfer any remaining funds first — this cannot be undone.</p>',
    async (modal) => {
      await api('POST', '/api/accounts/close', { accountNumber: modal.querySelector('#m-acc').value });
      toast('Account closed.');
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

document.getElementById('btn-download-csv').addEventListener('click', () => {
  const accNo = document.getElementById('tx-account-picker').value;
  if (!accNo) { toast('No account selected.', true); return; }
  if (txCache.length === 0) { toast('No transactions to export.', true); return; }
  downloadCsv(accNo + '-transactions.csv', txCache);
});

function csvEscape(v) {
  const s = String(v == null ? '' : v);
  return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}

function downloadCsv(filename, transactions) {
  const headers = ['Date', 'Voucher No.', 'Type', 'Description', 'Amount', 'Balance After'];
  const rows = transactions.slice().reverse().map(t => [
    t.timestamp.replace('T', ' ').substring(0, 19),
    t.id,
    txTypeLabel(t.type),
    t.description || '',
    t.amount.toFixed(2),
    t.balanceAfter.toFixed(2)
  ]);
  const csv = [headers, ...rows].map(row => row.map(csvEscape).join(',')).join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

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
    selectField('Disburse To', 'm-acc', openAccountsCache().map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
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
    selectField('From Account', 'm-acc', openAccountsCache().map(a => ({ value: a.accountNumber, label: a.accountNumber + ' — ' + labelType(a.type) }))) +
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

// ====================================================== CUSTOMER: PAYEES

async function loadBeneficiaries() {
  const data = await api('GET', '/api/beneficiaries');
  const wrap = document.getElementById('beneficiaries-table-wrap');
  if (data.beneficiaries.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No saved payees yet — add one above.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th>Nickname</th><th>Account Number</th><th>Added</th><th></th></tr></thead>
      <tbody>
        ${data.beneficiaries.map(b => `
          <tr>
            <td>${b.nickname}</td>
            <td class="tabular">${b.accountNumber}</td>
            <td>${b.addedOn}</td>
            <td><button class="btn btn-danger btn-sm" data-remove-ben="${b.id}" style="padding:5px 10px;">Remove</button></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('[data-remove-ben]').forEach(btn => {
    btn.addEventListener('click', async () => {
      try {
        await api('POST', '/api/beneficiaries/remove', { beneficiaryId: btn.dataset.removeBen });
        toast('Payee removed.');
        loadBeneficiaries();
      } catch (e) { toast(e.message, true); }
    });
  });
}

document.getElementById('btn-add-beneficiary').addEventListener('click', async () => {
  const nickname = document.getElementById('ben-nickname').value;
  const accountNumber = document.getElementById('ben-account').value;
  if (!nickname || !accountNumber) { toast('Both a nickname and account number are required.', true); return; }
  try {
    await api('POST', '/api/beneficiaries/add', { nickname, accountNumber });
    toast('Payee saved.');
    document.getElementById('ben-nickname').value = '';
    document.getElementById('ben-account').value = '';
    loadBeneficiaries();
  } catch (e) { toast(e.message, true); }
});

// ================================================ CUSTOMER: STANDING ORDERS

async function loadStandingOrders() {
  const accData = await api('GET', '/api/accounts');
  myAccountsCache = accData.accounts;
  document.getElementById('so-from').innerHTML = openAccountsCache()
    .map(a => `<option value="${a.accountNumber}">${a.accountNumber} — ${labelType(a.type)} — $${money(a.balance)}</option>`).join('')
    || '<option value="">Open an account first</option>';
  const startField = document.getElementById('so-start');
  if (!startField.value) startField.value = new Date().toISOString().substring(0, 10);

  const data = await api('GET', '/api/standing-orders');
  const wrap = document.getElementById('standing-orders-table-wrap');
  if (data.standingOrders.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No standing orders yet — set one up above.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th>From</th><th>To</th><th style="text-align:right">Amount</th><th>Frequency</th><th>Next Run</th><th>Status</th><th>Last Result</th><th></th></tr></thead>
      <tbody>
        ${data.standingOrders.map(so => `
          <tr>
            <td class="tabular">${so.fromAccount}</td>
            <td class="tabular">${so.toAccount}</td>
            <td class="num">$${money(so.amount)}</td>
            <td>${so.frequency}</td>
            <td>${so.active ? so.nextRunDate : '—'}</td>
            <td><span class="badge ${so.active ? 'badge-active' : 'badge-frozen'}">${so.active ? 'Active' : 'Cancelled'}</span></td>
            <td style="font-size:12px; color:var(--slate);">${so.lastResult || 'Not run yet'}</td>
            <td>${so.active ? `<button class="btn btn-danger btn-sm" data-cancel-so="${so.id}" style="padding:5px 10px;">Cancel</button>` : ''}</td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('[data-cancel-so]').forEach(btn => {
    btn.addEventListener('click', async () => {
      try {
        await api('POST', '/api/standing-orders/cancel', { standingOrderId: btn.dataset.cancelSo });
        toast('Standing order cancelled.');
        loadStandingOrders();
      } catch (e) { toast(e.message, true); }
    });
  });
}

document.getElementById('btn-create-standing-order').addEventListener('click', async () => {
  try {
    await api('POST', '/api/standing-orders/create', {
      fromAccount: document.getElementById('so-from').value,
      toAccount: document.getElementById('so-to').value,
      amount: document.getElementById('so-amount').value,
      frequency: document.getElementById('so-frequency').value,
      startDate: document.getElementById('so-start').value,
      note: document.getElementById('so-note').value
    });
    toast('Standing order created — it will run automatically on schedule.');
    document.getElementById('so-to').value = '';
    document.getElementById('so-amount').value = '';
    document.getElementById('so-note').value = '';
    loadStandingOrders();
  } catch (e) { toast(e.message, true); }
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

async function loadAdminOverview() {
  const data = await api('GET', '/api/admin/summary');
  document.getElementById('admin-ov-tiles').innerHTML = `
    <div class="tile"><div class="tile-label">Total Customers</div><div class="tile-value tabular">${data.totalCustomers}</div></div>
    <div class="tile"><div class="tile-label">Total Accounts</div><div class="tile-value tabular">${data.totalAccounts}</div></div>
    <div class="tile"><div class="tile-label">Total Deposits</div><div class="tile-value tabular">$${money(data.totalDeposits)}</div></div>
    <div class="tile"><div class="tile-label">Loans Issued</div><div class="tile-value tabular">${data.totalLoans}</div></div>
    <div class="tile"><div class="tile-label">Outstanding Loan Balance</div><div class="tile-value tabular">$${money(data.totalOutstandingLoans)}</div></div>
    <div class="tile"><div class="tile-label">Active Payroll Employees</div><div class="tile-value tabular">${data.totalEmployees}</div></div>
    <div class="tile"><div class="tile-label">Total Monthly Payroll</div><div class="tile-value tabular">$${money(data.totalMonthlyPayroll)}</div></div>
  `;

  const activityData = await api('GET', '/api/admin/recent-activity');
  const wrap = document.getElementById('admin-ov-recent-wrap');
  if (activityData.activity.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No activity yet.</div>`;
    return;
  }
  const inflow = new Set(['DEPOSIT', 'TRANSFER_IN', 'INTEREST_CREDIT', 'LOAN_DISBURSEMENT', 'SALARY_CREDIT']);
  wrap.innerHTML = activityData.activity.map(t => `
    <div class="recent-item">
      <div>
        <div class="recent-desc">${txTypeLabel(t.type)} &middot; ${t.accountNumber} (${t.owner})</div>
        <div class="recent-meta">${t.timestamp.replace('T', ' ').substring(0, 16)} &middot; ${t.description || ''}</div>
      </div>
      <div class="recent-amount ${inflow.has(t.type) ? 'positive' : 'negative'}">
        ${inflow.has(t.type) ? '+' : '−'}$${money(t.amount)}
      </div>
    </div>`).join('');
}

document.querySelectorAll('[data-jump]').forEach(btn => {
  btn.addEventListener('click', () => {
    const view = btn.dataset.jump;
    const nav = view.startsWith('admin-') ? document.getElementById('nav-admin') : document.getElementById('nav-customer');
    nav.querySelectorAll('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === view));
    showView(view);
  });
});

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
  wireTableFilter('customers-filter', 'admin-customers-table-wrap');
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
            <td><span class="badge ${a.closed ? 'badge-frozen' : a.frozen ? 'badge-pending' : 'badge-active'}">${a.closed ? 'Closed' : a.frozen ? 'Frozen' : 'Active'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedAccountNumber = row.dataset.acc; loadAdminAccounts(); });
  });
  wireTableFilter('admin-accounts-filter', 'admin-accounts-table-wrap');
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
  wireTableFilter('admin-loans-filter', 'admin-loans-table-wrap');
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

// ============================================================ ADMIN: PAYROLL

let selectedEmployeeId = null;
let allAccountsCache = [];

async function loadAdminPayroll() {
  const accData = await api('GET', '/api/admin/accounts');
  allAccountsCache = accData.accounts;
  const optionsHtml = allAccountsCache.map(a =>
    `<option value="${a.accountNumber}">${a.accountNumber} — ${a.owner} — ${labelType(a.type)} — $${money(a.balance)}</option>`
  ).join('');
  document.getElementById('pr-employee-account').innerHTML = optionsHtml || '<option value="">No accounts yet</option>';
  document.getElementById('pr-employer-account').innerHTML = optionsHtml || '<option value="">No accounts yet</option>';

  const data = await api('GET', '/api/admin/payroll/employees');
  const wrap = document.getElementById('admin-payroll-table-wrap');
  if (data.employees.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No employees registered yet — add one above.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>ID</th><th>Employee</th><th>Pays Into</th><th>Employer</th><th>Funded From</th><th>Position</th><th style="text-align:right">Salary</th><th>Last Paid</th><th>Status</th></tr></thead>
      <tbody>
        ${data.employees.map(e => `
          <tr class="selectable ${e.employeeId === selectedEmployeeId ? 'selected' : ''}" data-emp="${e.employeeId}">
            <td><input type="radio" name="emppick" ${e.employeeId === selectedEmployeeId ? 'checked' : ''}></td>
            <td class="tabular">${e.employeeId}</td>
            <td>${e.employeeName}</td>
            <td class="tabular">${e.accountNumber}</td>
            <td>${e.employerName}</td>
            <td class="tabular">${e.employerAccountNumber}</td>
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
  const employeeAcc = document.getElementById('pr-employee-account').value;
  const employerAcc = document.getElementById('pr-employer-account').value;
  if (!employeeAcc || !employerAcc) { toast('Both accounts are required — open one for the employer/employee first if missing.', true); return; }
  if (employeeAcc === employerAcc) { toast('The employee and employer cannot be the same account.', true); return; }
  try {
    await api('POST', '/api/admin/payroll/employees/add', {
      accountNumber: employeeAcc,
      employerAccountNumber: employerAcc,
      position: document.getElementById('pr-position').value,
      monthlySalary: document.getElementById('pr-salary').value
    });
    toast('Employee added to payroll.');
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
    if (data.paidCount === 0 && data.skippedCount === 0) {
      toast('No employees registered yet.', true);
      return;
    }
    toast(`Payroll run: ${data.paidCount} paid, ${data.skippedCount} skipped.`, data.paidCount === 0);
    const card = document.getElementById('payroll-results-card');
    card.classList.remove('hidden');
    const paidRows = data.payslips.map(ps => `
      <tr>
        <td class="tabular">${ps.voucherId}</td>
        <td>${ps.employeeName}</td>
        <td>${ps.employerName}</td>
        <td class="num">$${money(ps.amount)}</td>
        <td><span class="badge badge-active">Paid</span></td>
        <td><button class="btn btn-ghost btn-sm" data-payslip="${ps.voucherId}" style="padding:5px 10px;">Print Payslip</button></td>
      </tr>`).join('');
    const skippedRows = data.skipped.map(sk => `
      <tr>
        <td class="tabular">—</td>
        <td>${sk.employeeName}</td>
        <td>—</td>
        <td class="num">—</td>
        <td><span class="badge badge-frozen">Skipped</span></td>
        <td style="color:var(--slate); font-size:12px;">${sk.reason}</td>
      </tr>`).join('');
    document.getElementById('payroll-results-wrap').innerHTML = `
      <table class="ledger">
        <thead><tr><th>Voucher</th><th>Employee</th><th>Employer</th><th style="text-align:right">Amount</th><th>Result</th><th></th></tr></thead>
        <tbody>${paidRows}${skippedRows}</tbody>
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

async function loadAdminReports() {
  const data = await api('GET', '/api/admin/summary');
  lastReportData = data;
  document.getElementById('admin-report-tiles').innerHTML = `
    <div class="tile"><div class="tile-label">Total Customers</div><div class="tile-value tabular">${data.totalCustomers}</div></div>
    <div class="tile"><div class="tile-label">Total Accounts</div><div class="tile-value tabular">${data.totalAccounts}</div></div>
    <div class="tile"><div class="tile-label">Total Deposits</div><div class="tile-value tabular">$${money(data.totalDeposits)}</div></div>
    <div class="tile"><div class="tile-label">Loans Issued</div><div class="tile-value tabular">${data.totalLoans}</div></div>
    <div class="tile"><div class="tile-label">Outstanding Loan Balance</div><div class="tile-value tabular">$${money(data.totalOutstandingLoans)}</div></div>
    <div class="tile"><div class="tile-label">Active Payroll Employees</div><div class="tile-value tabular">${data.totalEmployees}</div></div>
    <div class="tile"><div class="tile-label">Total Monthly Payroll</div><div class="tile-value tabular">$${money(data.totalMonthlyPayroll)}</div></div>
  `;
}

let lastReportData = null;

function openBankReport(data) {
  const rows = [
    ['Total Customers', data.totalCustomers],
    ['Total Accounts', data.totalAccounts],
    ['Total Deposits', '$' + money(data.totalDeposits)],
    ['Loans Issued', data.totalLoans],
    ['Outstanding Loan Balance', '$' + money(data.totalOutstandingLoans)],
    ['Active Payroll Employees', data.totalEmployees],
    ['Total Monthly Payroll', '$' + money(data.totalMonthlyPayroll)],
  ];
  const body = `
    <div class="doc-grid">
      <div><div class="k">Report Date</div><div class="v">${new Date().toLocaleDateString()}</div></div>
      <div><div class="k">Report Type</div><div class="v">Bank Summary — All Branches</div></div>
    </div>
    <table class="doc-table">
      <thead><tr><th>Metric</th><th class="num">Value</th></tr></thead>
      <tbody>
        ${rows.map(([label, value]) => `<tr><td>${label}</td><td class="num">${value}</td></tr>`).join('')}
      </tbody>
    </table>
    <div class="doc-footer">
      <div class="doc-sign"><div class="line"></div><div class="label">Prepared By — Authorized Signature</div></div>
      <div></div>
    </div>`;
  openPrintable('Bank Summary Report', body);
}

document.getElementById('btn-download-report').addEventListener('click', async () => {
  const data = lastReportData || await api('GET', '/api/admin/summary');
  openBankReport(data);
});

// ============================================================== ADMIN: STAFF

let selectedStaffUsername = null;

async function loadAdminStaff() {
  const data = await api('GET', '/api/admin/staff');
  const wrap = document.getElementById('admin-staff-table-wrap');
  if (data.staff.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No staff accounts.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th></th><th>Username</th><th>Role</th></tr></thead>
      <tbody>
        ${data.staff.map(st => `
          <tr class="selectable ${st.username === selectedStaffUsername ? 'selected' : ''}" data-staff="${st.username}">
            <td><input type="radio" name="staffpick" ${st.username === selectedStaffUsername ? 'checked' : ''}></td>
            <td class="tabular">${st.username}</td>
            <td><span class="badge ${st.role === 'ADMIN' ? 'badge-active' : 'badge-pending'}">${st.role === 'ADMIN' ? 'Admin' : 'Teller'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>`;
  wrap.querySelectorAll('tr.selectable').forEach(row => {
    row.addEventListener('click', () => { selectedStaffUsername = row.dataset.staff; loadAdminStaff(); });
  });
  wireTableFilter('staff-filter', 'admin-staff-table-wrap');
}

document.getElementById('btn-add-staff').addEventListener('click', async () => {
  const username = document.getElementById('st-username').value;
  const password = document.getElementById('st-password').value;
  const role = document.getElementById('st-role').value;
  if (!username || !password) { toast('Username and password are required.', true); return; }
  try {
    await api('POST', '/api/admin/staff/add', { username, password, role });
    toast('Staff account created.');
    document.getElementById('st-username').value = '';
    document.getElementById('st-password').value = '';
    loadAdminStaff();
  } catch (e) { toast(e.message, true); }
});

document.getElementById('btn-remove-staff').addEventListener('click', async () => {
  if (!selectedStaffUsername) { toast('Select a staff account first.', true); return; }
  try {
    await api('POST', '/api/admin/staff/remove', { username: selectedStaffUsername });
    toast('Staff account removed.');
    selectedStaffUsername = null;
    loadAdminStaff();
  } catch (e) { toast(e.message, true); }
});

// ---------------------------------------------------------- generic table filter

/** Wires a text input to show/hide rows of a table whose text doesn't match, live as you type. */
function wireTableFilter(inputId, tableWrapId) {
  const input = document.getElementById(inputId);
  if (!input || input.dataset.wired) return;
  input.dataset.wired = '1';
  input.addEventListener('input', () => {
    const q = input.value.trim().toLowerCase();
    const wrap = document.getElementById(tableWrapId);
    if (!wrap) return;
    wrap.querySelectorAll('tbody tr').forEach(row => {
      row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
  });
}

// =============================================================== ADMIN: AUDIT

async function loadAdminAuditLog() {
  const data = await api('GET', '/api/admin/audit-log');
  const wrap = document.getElementById('admin-audit-table-wrap');
  if (data.entries.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No actions recorded yet.</div>`;
  } else {
    wrap.innerHTML = `
      <table class="ledger">
        <thead><tr><th>Time</th><th>Actor</th><th>Role</th><th>Action</th><th>Details</th></tr></thead>
        <tbody>
          ${data.entries.map(e => `
            <tr>
              <td class="tabular">${e.timestamp.replace('T', ' ').substring(0, 19)}</td>
              <td>${e.actor}</td>
              <td><span class="badge ${e.role === 'ADMIN' ? 'badge-active' : 'badge-pending'}">${e.role === 'ADMIN' ? 'Admin' : 'Teller'}</span></td>
              <td class="tabular">${e.action}</td>
              <td>${e.details}</td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  }
  wireTableFilter('audit-filter', 'admin-audit-table-wrap');
}
