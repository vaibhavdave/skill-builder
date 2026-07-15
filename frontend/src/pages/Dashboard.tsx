import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api, ApiError } from "../api";
import type { Skill } from "../types";
import { Button, Card, EmptyState, ErrorNote, Field, inputClass, Modal, Spinner } from "../components/ui";

export function Dashboard() {
  const queryClient = useQueryClient();
  const { data: skills, isLoading, error } = useQuery({ queryKey: ["skills"], queryFn: api.listSkills });
  const [editing, setEditing] = useState<Skill | "new" | null>(null);

  const remove = useMutation({
    mutationFn: api.deleteSkill,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["skills"] }),
  });

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Your skills</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Everything you're learning, with your books, courses and recommendations in one place.
          </p>
        </div>
        <Button onClick={() => setEditing("new")}>+ New skill</Button>
      </div>

      {isLoading && <Spinner />}
      {error && <ErrorNote message={error instanceof Error ? error.message : "Failed to load skills"} />}

      {skills && skills.length === 0 && (
        <EmptyState title="No skills yet">
          Create your first skill — for example “System Design” or “Spanish”.
        </EmptyState>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {skills?.map((skill) => (
          <Card key={skill.id} className="flex flex-col p-4">
            <Link to={`/skills/${skill.id}`} className="group flex-1">
              <h2 className="font-semibold group-hover:text-indigo-600 dark:group-hover:text-indigo-400">
                {skill.name}
              </h2>
              {skill.description && (
                <p className="mt-1 line-clamp-3 text-sm text-slate-500 dark:text-slate-400">
                  {skill.description}
                </p>
              )}
            </Link>
            <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
              <span>
                📄 {skill.documentCount} · 🔗 {skill.courseLinkCount} · ⭐ {skill.recommendationCount}
              </span>
              <span className="flex gap-2">
                <button
                  className="hover:text-indigo-600 dark:hover:text-indigo-400"
                  onClick={() => setEditing(skill)}
                >
                  Edit
                </button>
                <button
                  className="hover:text-rose-600 dark:hover:text-rose-400"
                  onClick={() => {
                    if (confirm(`Delete "${skill.name}" and everything saved under it?`)) {
                      remove.mutate(skill.id);
                    }
                  }}
                >
                  Delete
                </button>
              </span>
            </div>
          </Card>
        ))}
      </div>

      {editing && (
        <SkillDialog
          skill={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function SkillDialog({ skill, onClose }: { skill: Skill | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(skill?.name ?? "");
  const [description, setDescription] = useState(skill?.description ?? "");
  const [error, setError] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: () => {
      const body = { name: name.trim(), description: description.trim() || null };
      return skill ? api.updateSkill(skill.id, body) : api.createSkill(body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["skills"] });
      onClose();
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Something went wrong"),
  });

  return (
    <Modal title={skill ? "Edit skill" : "New skill"} onClose={onClose}>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (name.trim()) save.mutate();
        }}
      >
        <Field label="Name">
          <input
            className={inputClass}
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Distributed systems"
            autoFocus
            required
            maxLength={200}
          />
        </Field>
        <Field label="Description (optional)">
          <textarea
            className={inputClass}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What do you want to achieve?"
            rows={3}
          />
        </Field>
        {error && <ErrorNote message={error} />}
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={save.isPending || !name.trim()}>
            {save.isPending ? <Spinner /> : skill ? "Save" : "Create"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
