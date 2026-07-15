export interface Skill {
  id: number;
  name: string;
  description: string | null;
  documentCount: number;
  courseLinkCount: number;
  recommendationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Document {
  id: number;
  skillId: number;
  title: string | null;
  originalFilename: string;
  contentType: string | null;
  sizeBytes: number;
  notes: string | null;
  uploadedAt: string;
}

export type CourseStatus = "TO_START" | "IN_PROGRESS" | "COMPLETED";

export interface CourseLink {
  id: number;
  skillId: number;
  url: string;
  title: string | null;
  provider: string | null;
  notes: string | null;
  status: CourseStatus;
  createdAt: string;
}

export type ResourceType = "BOOK" | "COURSE" | "YOUTUBE_VIDEO";

export interface Recommendation {
  id: number;
  resourceType: ResourceType;
  title: string;
  creator: string | null;
  reason: string | null;
  stars: number;
  searchQuery: string | null;
  generatedAt: string;
}
