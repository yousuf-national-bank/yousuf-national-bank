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
    showView('accounts');
    document.querySelectorAll('#nav-customer .nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === 'accounts'));
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
    accounts: loadAccounts,
    transactions: loadTransactionsView,
    loans: loadLoans,
    profile: loadProfile,
    'admin-customers': loadAdminCustomers,
    'admin-accounts': loadAdminAccounts,
    'admin-loans': loadAdminLoans,
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

async function loadTransactionsTable() {
  const accNo = document.getElementById('tx-account-picker').value;
  if (!accNo) return;
  const data = await api('GET', '/api/transactions', { accountNumber: accNo });
  const wrap = document.getElementById('tx-table-wrap');
  if (data.transactions.length === 0) {
    wrap.innerHTML = `<div class="empty-state">No transactions yet on this account.</div>`;
    return;
  }
  wrap.innerHTML = `
    <table class="ledger">
      <thead><tr><th>Date</th><th>Type</th><th style="text-align:right">Amount</th><th style="text-align:right">Balance After</th><th>Description</th></tr></thead>
      <tbody>
        ${data.transactions.slice().reverse().map(t => `
          <tr>
            <td class="tabular">${t.timestamp.replace('T', ' ').substring(0, 19)}</td>
            <td>${t.type.replace(/_/g, ' ')}</td>
            <td class="num">$${money(t.amount)}</td>
            <td class="num">$${money(t.balanceAfter)}</td>
            <td>${t.description || ''}</td>
          </tr>`).join('')}
      </tbody>
    </table>`;
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
