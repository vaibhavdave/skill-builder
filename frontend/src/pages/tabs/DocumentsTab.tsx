import { useRef, useState, type DragEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../api";
import { Button, Card, EmptyState, ErrorNote, Spinner } from "../../components/ui";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function fileIcon(contentType: string | null, filename: string): string {
  if (contentType?.includes("pdf") || filename.toLowerCase().endsWith(".pdf")) return "📕";
  if (/\.(docx?|odt)$/i.test(filename)) return "📘";
  if (/\.(epub|mobi)$/i.test(filename)) return "📗";
  if (/\.(txt|md)$/i.test(filename)) return "📝";
  return "📄";
}

export function DocumentsTab({ skillId }: { skillId: number }) {
  const queryClient = useQueryClient();
  const [dragActive, setDragActive] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { data: documents, isLoading, error } = useQuery({
    queryKey: ["documents", skillId],
    queryFn: () => api.listDocuments(skillId),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["documents", skillId] });
    queryClient.invalidateQueries({ queryKey: ["skills"] });
  };

  const upload = useMutation({
    mutationFn: (files: File[]) =>
      Promise.all(files.map((file) => api.uploadDocument(skillId, file))),
    onSuccess: invalidate,
    onError: (e) => setUploadError(e instanceof Error ? e.message : "Upload failed"),
  });

  const remove = useMutation({
    mutationFn: api.deleteDocument,
    onSuccess: invalidate,
  });

  const handleFiles = (list: FileList | null) => {
    if (!list || list.length === 0) return;
    setUploadError(null);
    upload.mutate(Array.from(list));
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    handleFiles(e.dataTransfer.files);
  };

  return (
    <div className="space-y-4">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragActive(true);
        }}
        onDragLeave={() => setDragActive(false)}
        onDrop={onDrop}
        onClick={() => inputRef.current?.click()}
        className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed p-8 text-center transition-colors ${
          dragActive
            ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-950/40"
            : "border-slate-300 hover:border-indigo-400 dark:border-slate-700"
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => {
            handleFiles(e.target.files);
            e.target.value = "";
          }}
        />
        <p className="font-medium">
          {upload.isPending ? <Spinner /> : "Drop books, PDFs or docs here"}
        </p>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">or click to choose files</p>
      </div>

      {uploadError && <ErrorNote message={uploadError} />}
      {error && <ErrorNote message={error instanceof Error ? error.message : "Failed to load documents"} />}
      {isLoading && <Spinner />}

      {documents && documents.length === 0 && (
        <EmptyState title="No documents yet">
          Upload the books and reference material you want to come back to.
        </EmptyState>
      )}

      <div className="space-y-2">
        {documents?.map((doc) => {
          const isPdf =
            doc.contentType?.includes("pdf") || doc.originalFilename.toLowerCase().endsWith(".pdf");
          return (
            <Card key={doc.id} className="flex items-center gap-3 p-3">
              <span className="text-2xl" aria-hidden>
                {fileIcon(doc.contentType, doc.originalFilename)}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{doc.title ?? doc.originalFilename}</p>
                <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                  {doc.originalFilename} · {formatSize(doc.sizeBytes)} ·{" "}
                  {new Date(doc.uploadedAt).toLocaleDateString()}
                </p>
              </div>
              <div className="flex shrink-0 gap-2">
                {isPdf && (
                  <a href={api.documentViewUrl(doc.id)} target="_blank" rel="noreferrer">
                    <Button variant="secondary">View</Button>
                  </a>
                )}
                <a href={api.documentDownloadUrl(doc.id)}>
                  <Button variant="secondary">Download</Button>
                </a>
                <Button
                  variant="danger"
                  onClick={() => {
                    if (confirm(`Delete "${doc.title ?? doc.originalFilename}"?`)) {
                      remove.mutate(doc.id);
                    }
                  }}
                >
                  Delete
                </Button>
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
