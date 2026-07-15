export function StarRating({ stars }: { stars: number }) {
  const clamped = Math.max(0, Math.min(5, stars));
  return (
    <span
      aria-label={`${clamped} out of 5 stars`}
      title={`${clamped}/5`}
      className="text-amber-500 dark:text-amber-400"
    >
      {"★".repeat(clamped)}
      <span className="text-slate-300 dark:text-slate-600">{"★".repeat(5 - clamped)}</span>
    </span>
  );
}
