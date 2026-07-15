import type { CourseLink, CourseStatus, Document, Recommendation, Skill } from "./types";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init);
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const problem = await response.json();
      if (problem && typeof problem.detail === "string") detail = problem.detail;
    } catch {
      // not a problem+json body
    }
    throw new ApiError(response.status, detail);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function json(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}

export const api = {
  listSkills: () => request<Skill[]>("/api/skills"),
  getSkill: (id: number) => request<Skill>(`/api/skills/${id}`),
  createSkill: (body: { name: string; description: string | null }) =>
    request<Skill>("/api/skills", json("POST", body)),
  updateSkill: (id: number, body: { name: string; description: string | null }) =>
    request<Skill>(`/api/skills/${id}`, json("PUT", body)),
  deleteSkill: (id: number) => request<void>(`/api/skills/${id}`, { method: "DELETE" }),

  listDocuments: (skillId: number) => request<Document[]>(`/api/skills/${skillId}/documents`),
  uploadDocument: (skillId: number, file: File, title?: string, notes?: string) => {
    const form = new FormData();
    form.append("file", file);
    if (title) form.append("title", title);
    if (notes) form.append("notes", notes);
    return request<Document>(`/api/skills/${skillId}/documents`, { method: "POST", body: form });
  },
  updateDocument: (id: number, body: { title?: string; notes?: string }) =>
    request<Document>(`/api/documents/${id}`, json("PUT", body)),
  deleteDocument: (id: number) => request<void>(`/api/documents/${id}`, { method: "DELETE" }),
  documentViewUrl: (id: number) => `/api/documents/${id}/view`,
  documentDownloadUrl: (id: number) => `/api/documents/${id}/download`,

  listLinks: (skillId: number) => request<CourseLink[]>(`/api/skills/${skillId}/links`),
  createLink: (
    skillId: number,
    body: { url: string; title?: string; provider?: string; notes?: string; status?: CourseStatus },
  ) => request<CourseLink>(`/api/skills/${skillId}/links`, json("POST", body)),
  updateLink: (
    id: number,
    body: { url: string; title?: string | null; provider?: string | null; notes?: string | null; status?: CourseStatus },
  ) => request<CourseLink>(`/api/links/${id}`, json("PUT", body)),
  deleteLink: (id: number) => request<void>(`/api/links/${id}`, { method: "DELETE" }),

  listRecommendations: (skillId: number) =>
    request<Recommendation[]>(`/api/skills/${skillId}/recommendations`),
  generateRecommendations: (skillId: number) =>
    request<Recommendation[]>(`/api/skills/${skillId}/recommendations/generate`, { method: "POST" }),
};
