import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import type { CourseLink, CourseStatus } from "../../types";
import { StatusBadge } from "../../components/StatusBadge";
import { Button, Card, EmptyState, ErrorNote, Field, inputClass, Spinner } from "../../components/ui";

const NEXT_STATUS: Record<CourseStatus, CourseStatus> = {
  TO_START: "IN_PROGRESS",
  IN_PROGRESS: "COMPLETED",
  COMPLETED: "TO_START",
};

export function CoursesTab({ skillId }: { skillId: number }) {
  const queryClient = useQueryClient();
  const [url, setUrl] = useState("");
  const [title, setTitle] = useState("");
  const [provider, setProvider] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: links, isLoading, error } = useQuery({
    queryKey: ["links", skillId],
    queryFn: () => api.listLinks(skillId),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["links", skillId] });
    queryClient.invalidateQueries({ queryKey: ["skills"] });
  };

  const create = useMutation({
    mutationFn: () =>
      api.createLink(skillId, {
        url: url.trim(),
        title: title.trim() || undefined,
        provider: provider.trim() || undefined,
      }),
    onSuccess: () => {
      setUrl("");
      setTitle("");
      setProvider("");
      setFormError(null);
      invalidate();
    },
    onError: (e) => setFormError(e instanceof Error ? e.message : "Could not save link"),
  });

  const cycleStatus = useMutation({
    mutationFn: (link: CourseLink) =>
      api.updateLink(link.id, {
        url: link.url,
        title: link.title,
        provider: link.provider,
        notes: link.notes,
        status: NEXT_STATUS[link.status],
      }),
    onSuccess: invalidate,
  });

  const remove = useMutation({ mutationFn: api.deleteLink, onSuccess: invalidate });

  return (
    <div className="space-y-4">
      <Card className="p-4">
        <form
          className="grid gap-3 sm:grid-cols-[2fr_1.5fr_1fr_auto] sm:items-end"
          onSubmit={(e) => {
            e.preventDefault();
            if (url.trim()) create.mutate();
          }}
        >
          <Field label="Course URL">
            <input
              className={inputClass}
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://…"
              required
            />
          </Field>
          <Field label="Title (optional)">
            <input
              className={inputClass}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Course name"
            />
          </Field>
          <Field label="Provider (optional)">
            <input
              className={inputClass}
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
              placeholder="Udemy, YouTube…"
            />
          </Field>
          <div className="mb-3">
            <Button type="submit" disabled={create.isPending || !url.trim()}>
              {create.isPending ? <Spinner /> : "Add"}
            </Button>
          </div>
        </form>
        {formError && <ErrorNote message={formError} />}
      </Card>

      {error && <ErrorNote message={error instanceof Error ? error.message : "Failed to load links"} />}
      {isLoading && <Spinner />}

      {links && links.length === 0 && (
        <EmptyState title="No courses saved yet">
          Paste links to the courses you plan to study and track your progress on each.
        </EmptyState>
      )}

      <div className="space-y-2">
        {links?.map((link) => (
          <Card key={link.id} className="flex items-center gap-3 p-3">
            <span className="text-2xl" aria-hidden>
              🎓
            </span>
            <div className="min-w-0 flex-1">
              <a
                href={link.url}
                target="_blank"
                rel="noreferrer"
                className="block truncate font-medium hover:text-indigo-600 dark:hover:text-indigo-400"
              >
                {link.title || link.url}
              </a>
              <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                {link.provider ? `${link.provider} · ` : ""}
                {link.url}
              </p>
            </div>
            <StatusBadge status={link.status} onClick={() => cycleStatus.mutate(link)} />
            <Button
              variant="danger"
              onClick={() => {
                if (confirm(`Remove "${link.title || link.url}"?`)) remove.mutate(link.id);
              }}
            >
              Delete
            </Button>
          </Card>
        ))}
      </div>
    </div>
  );
}
