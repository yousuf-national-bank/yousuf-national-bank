# Java Banking System — Web Edition

A full banking web app: open `http://localhost:8080` in any browser after
running a single Java command. No Node, no framework, no external
dependencies — the backend is a plain Java HTTP server (`com.sun.net.httpserver`,
built into the JDK) serving a hand-written HTML/CSS/JS frontend.

## How to run it

```bash
# 1. Compile everything
javac -d out $(find src -name "*.java")

# 2. Run the server (run this from the project root, so it can find the
#    "webroot" folder next to it)
java -cp out bank.web.WebServer
```

You'll see:
```
=================================================
 Yousuf National Bank web server is running!
 Open this in your browser: http://localhost:8080
 Press Ctrl+C to stop.
=================================================
```

Open **http://localhost:8080** in Chrome/Firefox/Edge/Safari. That's it.

Press `Ctrl+C` in the terminal to stop the server.

### Running from VS Code
Open `src/bank/web/WebServer.java`, click the ▶ Run link above its `main`
method — same as the console/GUI versions. Just make sure VS Code's working
directory is the project root (where the `webroot` folder lives) so the
server can find the HTML/CSS/JS files; if it can't, you'll get a 500 error
on `/`.

## What you get

**Login page** (`/`) — tabs for Customer Login, Register, and Admin Login
(default admin: `admin` / `admin123`).

**Customer dashboard**
- Accounts — open Savings/Checking/Fixed-Deposit, deposit, withdraw, transfer
- Transactions — full history per account
- Loans — apply, view status, repay
- Profile — edit details, change PIN

**Admin dashboard**
- Customers — lock/unlock
- Accounts — freeze/unfreeze, apply interest to all accounts
- Loans — approve/reject pending applications
- Reports — bank-wide totals

Every action is a real HTTP request to the Java backend, which updates the
in-memory `Bank` object and immediately saves it to `webbankdata.ser` in
your working directory (same file-based approach as the original console
app) — so your data is still there the next time you start the server.

## How it works under the hood

- `bank.web.WebServer` — starts an `HttpServer` on port 8080, routes
  `/api/...` requests to handler methods, and serves static files
  (`index.html`/`style.css`/`app.js`) from the `webroot` folder for
  everything else.
- `bank.web.AppState` — loads/saves the shared `Bank` object using the same
  `FileStorage` class the console app uses.
- `bank.web.WebSession` — a simple in-memory session (cookie-based) mapping
  a random token to a logged-in customer or admin.
- `bank.web.Json` — a tiny hand-rolled JSON writer (no external library).
- `webroot/app.js` — a small vanilla-JS single-page app: it calls the
  `/api/...` endpoints with `fetch()` and re-renders the page — no React,
  no build step, just a `<script>` tag.

The same underlying `Bank`, `Account`, `Customer`, `Loan` classes from the
original console/GUI versions power this too — only the "front door"
changed.

## API reference (if you want to build on it)

All endpoints are under `/api/`, return JSON, and use a session cookie
(`SID`) set on login.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/register` | none | create a customer |
| POST | `/api/login` | none | customer login |
| POST | `/api/admin-login` | none | admin login |
| POST | `/api/logout` | any | clear session |
| GET  | `/api/me` | any | current session info |
| GET  | `/api/accounts` | customer | list own accounts |
| POST | `/api/accounts/open` | customer | open an account |
| POST | `/api/accounts/deposit` | customer | deposit |
| POST | `/api/accounts/withdraw` | customer | withdraw |
| POST | `/api/accounts/transfer` | customer | transfer funds |
| GET  | `/api/transactions` | customer | history for one account |
| GET  | `/api/loans` | customer | list own loans |
| POST | `/api/loans/apply` | customer | apply for a loan |
| POST | `/api/loans/repay` | customer | repay a loan |
| POST | `/api/profile/update` | customer | edit profile |
| POST | `/api/profile/change-pin` | customer | change PIN |
| GET  | `/api/admin/customers` | admin | list customers |
| GET  | `/api/admin/accounts` | admin | list accounts |
| GET  | `/api/admin/loans` | admin | list loans |
| GET  | `/api/admin/summary` | admin | bank totals |
| POST | `/api/admin/customers/toggle-lock` | admin | lock/unlock a customer |
| POST | `/api/admin/accounts/toggle-freeze` | admin | freeze/unfreeze an account |
| POST | `/api/admin/accounts/apply-interest` | admin | credit interest to all |
| POST | `/api/admin/loans/approve` | admin | approve a loan |
| POST | `/api/admin/loans/reject` | admin | reject a loan |

## Verified working

I ran the compiled server and hit it with real HTTP requests end-to-end:
register → login → open account → deposit → apply for loan → admin login →
approve loan → bank summary, plus error cases (bad login, overdraft
protection, unauthenticated access, logout) — all returned the correct
JSON and no server exceptions. The frontend HTML/CSS/JS were also checked
for balanced tags and valid JavaScript syntax.

## Deploying it so others can reach it online

Right now it only listens on your machine (`localhost:8080`). To make it
reachable from any browser on the internet, you'd deploy the compiled
`out/` folder + `webroot/` to a small server (a $5 VPS, Render, Railway,
etc.), run the same `java -cp out bank.web.WebServer` command there, and
either open the port or put a reverse proxy (e.g. Caddy/Nginx) with HTTPS
in front of it. Happy to help set that up if you want to take it that far.

## Ideas for extending it further
- Swap `FileStorage` for the SQLite `bank.db` layer already in this project
  (see the GUI+Database README) for proper relational storage
- Add HTTPS (via a reverse proxy, or `com.sun.net.httpserver.HttpsServer`)
- Replace the hand-rolled session store with something like JWT if you
  deploy across multiple server instances
- Add charts to the admin Reports page (e.g. Chart.js via CDN)
