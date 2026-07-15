import { useTheme, type Theme } from "../theme";

const OPTIONS: { value: Theme; label: string; icon: string }[] = [
  { value: "light", label: "Light", icon: "☀️" },
  { value: "dark", label: "Dark", icon: "🌙" },
  { value: "system", label: "System", icon: "💻" },
];

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  return (
    <div
      role="radiogroup"
      aria-label="Theme"
      className="flex items-center gap-0.5 rounded-full border border-slate-200 bg-white p-0.5 dark:border-slate-700 dark:bg-slate-900"
    >
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          role="radio"
          aria-checked={theme === option.value}
          title={option.label}
          onClick={() => setTheme(option.value)}
          className={`rounded-full px-2.5 py-1 text-sm transition-colors ${
            theme === option.value
              ? "bg-slate-200 dark:bg-slate-700"
              : "hover:bg-slate-100 dark:hover:bg-slate-800"
          }`}
        >
          <span aria-hidden>{option.icon}</span>
          <span className="sr-only">{option.label}</span>
        </button>
      ))}
    </div>
  );
}
