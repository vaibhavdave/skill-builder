import { Link, Route, Routes } from "react-router-dom";
import { ThemeToggle } from "./components/ThemeToggle";
import { Dashboard } from "./pages/Dashboard";
import { SkillDetail } from "./pages/SkillDetail";

export default function App() {
  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/80 backdrop-blur dark:border-slate-800 dark:bg-slate-950/80">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-semibold tracking-tight">
            🎯 Skill Builder
          </Link>
          <ThemeToggle />
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/skills/:id" element={<SkillDetail />} />
        </Routes>
      </main>
    </div>
  );
}
