import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import { ErrorNote, Spinner } from "../components/ui";
import { DocumentsTab } from "./tabs/DocumentsTab";
import { CoursesTab } from "./tabs/CoursesTab";
import { RecommendedTab } from "./tabs/RecommendedTab";

type Tab = "documents" | "courses" | "recommended";

const TABS: { id: Tab; label: string }[] = [
  { id: "documents", label: "📄 Documents" },
  { id: "courses", label: "🔗 Courses" },
  { id: "recommended", label: "⭐ Recommended" },
];

export function SkillDetail() {
  const { id } = useParams();
  const skillId = Number(id);
  const [tab, setTab] = useState<Tab>("documents");

  const { data: skill, isLoading, error } = useQuery({
    queryKey: ["skills", skillId],
    queryFn: () => api.getSkill(skillId),
    enabled: Number.isFinite(skillId),
  });

  if (isLoading) return <Spinner />;
  if (error || !skill) {
    return <ErrorNote message={error instanceof Error ? error.message : "Skill not found"} />;
  }

  return (
    <div>
      <nav className="mb-2 text-sm text-slate-500 dark:text-slate-400">
        <Link to="/" className="hover:text-indigo-600 dark:hover:text-indigo-400">
          Skills
        </Link>{" "}
        / {skill.name}
      </nav>
      <h1 className="text-2xl font-bold">{skill.name}</h1>
      {skill.description && (
        <p className="mt-1 max-w-2xl text-sm text-slate-500 dark:text-slate-400">{skill.description}</p>
      )}

      <div className="mt-6 border-b border-slate-200 dark:border-slate-800">
        <div role="tablist" className="-mb-px flex gap-1">
          {TABS.map((t) => (
            <button
              key={t.id}
              role="tab"
              aria-selected={tab === t.id}
              onClick={() => setTab(t.id)}
              className={`rounded-t-lg border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
                tab === t.id
                  ? "border-indigo-600 text-indigo-600 dark:border-indigo-400 dark:text-indigo-400"
                  : "border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="pt-5">
        {tab === "documents" && <DocumentsTab skillId={skillId} />}
        {tab === "courses" && <CoursesTab skillId={skillId} />}
        {tab === "recommended" && <RecommendedTab skillId={skillId} />}
      </div>
    </div>
  );
}
