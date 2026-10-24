import "./App.css";

export default function App() {
  return (
    <div className="app">
      <header className="top-bar">
        <div>
          <span className="app-name">ProHo</span>
          <span className="tagline">Administración de propiedad horizontal</span>
        </div>
      </header>
      <main className="content">
        {/* Placeholder until memberships come from the API (Phase 1). */}
        <span className="chip">Sin comunidad</span>
        <h1>Bienvenido a ProHo</h1>
        <p>
          Aquí verá los estados de cuenta, anuncios y mensajes de sus
          comunidades.
        </p>
      </main>
    </div>
  );
}
