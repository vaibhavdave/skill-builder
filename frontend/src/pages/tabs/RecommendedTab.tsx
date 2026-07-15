import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../../api";
import type { Recommendation, ResourceType } from "../../types";
import { StarRating } from "../../components/StarRating";
import { Button, Card, EmptyState, ErrorNote, Spinner } from "../../components/ui";

const SECTIONS: { type: ResourceType; label: string; icon: string }[] = [
  { type: "BOOK", label: "Books", icon: "📚" },
  { type: "COURSE", label: "Courses", icon: "🎓" },
  { type: "YOUTUBE_VIDEO", label: "YouTube", icon: "▶️" },
];

function findItUrl(rec: Recommendation): string {
  const query = rec.searchQuery || `${rec.title} ${rec.creator ?? ""}`;
  if (rec.resourceType === "YOUTUBE_VIDEO") {
    return `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`;
  }
  return `https://www.google.com/search?q=${encodeURIComponent(query)}`;
}

export function RecommendedTab({ skillId }: { skillId: number }) {
  const queryClient = useQueryClient();
  const [generateError, setGenerateError] = useState<string | null>(null);

  const { data: recommendations, isLoading, error } = useQuery({
    queryKey: ["recommendations", skillId],
    queryFn: () => api.listRecommendations(skillId),
  });

  const generate = useMutation({
    mutationFn: () => api.generateRecommendations(skillId),
    onSuccess: (fresh) => {
      setGenerateError(null);
      queryClient.setQueryData(["recommendations", skillId], fresh);
      queryClient.invalidateQueries({ queryKey: ["skills"] });
    },
    onError: (e) =>
      setGenerateError(e instanceof ApiError ? e.message : "Could not generate recommendations"),
  });

  const hasResults = (recommendations?.length ?? 0) > 0;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Claude curates the best books, courses and YouTube videos for this skill and rates each
          one by how valuable it is for learning.
        </p>
        <Button onClick={() => generate.mutate()} disabled={generate.isPending}>
          {generate.isPending ? (
            <>
              <Spinner /> Asking Claude…
            </>
          ) : hasResults ? (
            "↻ Regenerate"
          ) : (
            "✨ Generate with Claude"
          )}
        </Button>
      </div>

      {generateError && <ErrorNote message={generateError} />}
      {error && (
        <ErrorNote message={error instanceof Error ? error.message : "Failed to load recommendations"} />
      )}
      {isLoading && <Spinner />}

      {!isLoading && !hasResults && !generate.isPending && (
        <EmptyState title="No recommendations yet">
          Click <strong>Generate with Claude</strong> to get a star-rated reading and watching list
          for this skill. Requires <code>ANTHROPIC_API_KEY</code> on the backend.
        </EmptyState>
      )}

      {hasResults &&
        SECTIONS.map(({ type, label, icon }) => {
          const items = recommendations!.filter((r) => r.resourceType === type);
          if (items.length === 0) return null;
          return (
            <section key={type}>
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                {icon} {label}
              </h3>
              <div className="space-y-2">
                {items.map((rec) => (
                  <Card key={rec.id} className="flex items-start gap-3 p-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-x-2">
                        <span className="font-medium">{rec.title}</span>
                        <StarRating stars={rec.stars} />
                      </div>
                      {rec.creator && (
                        <p className="text-xs text-slate-500 dark:text-slate-400">by {rec.creator}</p>
                      )}
                      {rec.reason && (
                        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{rec.reason}</p>
                      )}
                    </div>
                    <a href={findItUrl(rec)} target="_blank" rel="noreferrer" className="shrink-0">
                      <Button variant="secondary">Find it ↗</Button>
                    </a>
                  </Card>
                ))}
              </div>
            </section>
          );
        })}

      {hasResults && (
        <p className="text-xs text-slate-400 dark:text-slate-500">
          Generated {new Date(recommendations![0].generatedAt).toLocaleString()} · “Find it” opens a
          search so links never go stale.
        </p>
      )}
    </div>
  );
}
