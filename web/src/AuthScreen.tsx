import { useState, type FormEvent } from "react";
import { ApiError, login, registerAccount } from "./api";

interface AuthScreenProps {
  onLoggedIn: (token: string) => void;
}

export default function AuthScreen({ onLoggedIn }: AuthScreenProps) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === "register") {
        await registerAccount(email, password, displayName);
      }
      const { token } = await login(email, password);
      onLoggedIn(token);
    } catch (err) {
      setError(err instanceof ApiError ? describeError(err.code) : "No se pudo conectar con el servidor.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="content">
      <span className="chip">Sin comunidad</span>
      <h1>{mode === "login" ? "Iniciar sesión" : "Crear cuenta"}</h1>
      <form className="form" onSubmit={handleSubmit}>
        {mode === "register" && (
          <label className="field">
            Nombre
            <input
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              maxLength={200}
            />
          </label>
        )}
        <label className="field">
          Correo electrónico
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            maxLength={320}
          />
        </label>
        <label className="field">
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={72}
          />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={busy}>
          {mode === "login" ? "Iniciar sesión" : "Crear cuenta"}
        </button>
      </form>
      <p className="form-switch">
        {mode === "login" ? (
          <>
            ¿No tiene cuenta?{" "}
            <button type="button" className="link" onClick={() => setMode("register")}>
              Cree una
            </button>
          </>
        ) : (
          <>
            ¿Ya tiene cuenta?{" "}
            <button type="button" className="link" onClick={() => setMode("login")}>
              Inicie sesión
            </button>
          </>
        )}
      </p>
    </div>
  );
}

function describeError(code: string): string {
  switch (code) {
    case "invalid_credentials":
      return "Correo o contraseña incorrectos.";
    case "email_already_registered":
      return "Ese correo ya está registrado.";
    case "invalid_request":
      return "Revise los datos ingresados.";
    default:
      return "Ocurrió un error inesperado.";
  }
}
