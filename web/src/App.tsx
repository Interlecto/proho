import { useEffect, useState } from "react";
import "./App.css";
import AuthScreen from "./AuthScreen";
import CommunitiesScreen from "./CommunitiesScreen";
import { me, logout as apiLogout, type Account } from "./api";

const TOKEN_STORAGE_KEY = "proho.token";

export default function App() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_STORAGE_KEY),
  );
  const [account, setAccount] = useState<Account | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);

  useEffect(() => {
    if (!token) {
      setCheckingSession(false);
      return;
    }
    me(token)
      .then(setAccount)
      .catch(() => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken(null);
      })
      .finally(() => setCheckingSession(false));
  }, [token]);

  function handleLoggedIn(newToken: string) {
    localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
    setToken(newToken);
  }

  function handleLogout() {
    if (token) {
      apiLogout(token).catch(() => {
        // Best effort; the token is being discarded locally regardless.
      });
    }
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken(null);
    setAccount(null);
  }

  return (
    <div className="app">
      <header className="top-bar">
        <div>
          <svg
            className="app-mark"
            viewBox="12 18 163 87"
            role="img"
            aria-label="ProHo"
          >
            <path
              fill="currentColor"
              d="M 14 20 L 14 100 L 30 100 L 30 36 L 40 36 A 9 9 0 0 1 49 45 A 9 9 0 0 1 40 54 L 63.323828 54 A 25 25 0 0 0 65 45 A 25 25 0 0 0 40 20 L 14 20 z M 95 20 L 95 53.278906 A 27.5 27.5 0 0 1 115.31016 73.880078 A 6.5 6.5 0 0 1 117.5 73.5 A 6.5 6.5 0 0 1 124 80 L 124 100 L 140 100 L 140 80 A 22.5 22.5 0 0 0 117.5 57.5 A 22.5 22.5 0 0 0 111 58.458984 L 111 20 L 95 20 z M 62.5 57.5 A 22.5 22.5 0 0 0 52.192188 60 L 40 60 L 40 100 L 56 100 L 56 80 A 6.5 6.5 0 0 1 62.5 73.5 A 6.5 6.5 0 0 1 67.7 76.1 L 80.5 66.5 A 22.5 22.5 0 0 0 62.5 57.5 z M 88.5 57.5 L 88.5 73.5 A 6.5 6.5 0 0 1 95 80 A 6.5 6.5 0 0 1 88.5 86.5 A 6.5 6.5 0 0 1 82 80 L 66 80 A 22.5 22.5 0 0 0 88.5 102.5 A 22.5 22.5 0 0 0 111 80 A 22.5 22.5 0 0 0 88.5 57.5 z M 150 57.5 A 22.5 22.5 0 0 0 136.5 62 L 146.1 74.8 A 6.5 6.5 0 0 1 150 73.5 A 6.5 6.5 0 0 1 156.5 80 A 6.5 6.5 0 0 1 150 86.5 A 6.5 6.5 0 0 1 146 85.123047 L 146 102.14102 A 22.5 22.5 0 0 0 150 102.5 A 22.5 22.5 0 0 0 172.5 80 A 22.5 22.5 0 0 0 150 57.5 z "
            />
          </svg>
          <span className="tagline">Administración de propiedad horizontal</span>
        </div>
      </header>
      <main>
        {checkingSession ? (
          <div className="content">
            <p>Cargando…</p>
          </div>
        ) : token && account ? (
          <CommunitiesScreen token={token} account={account} onLogout={handleLogout} />
        ) : (
          <AuthScreen onLoggedIn={handleLoggedIn} />
        )}
      </main>
    </div>
  );
}
