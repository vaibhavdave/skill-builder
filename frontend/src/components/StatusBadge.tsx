import type { CourseStatus } from "../types";

export const STATUS_LABELS: Record<CourseStatus, string> = {
  TO_START: "To start",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
};

const STATUS_STYLES: Record<CourseStatus, string> = {
  TO_START:
    "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300",
  IN_PROGRESS:
    "bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300",
  COMPLETED:
    "bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300",
};

export function StatusBadge({
  status,
  onClick,
}: {
  status: CourseStatus;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title="Click to change status"
      className={`rounded-full px-2.5 py-0.5 text-xs font-medium transition-opacity hover:opacity-80 ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </button>
  );
}
