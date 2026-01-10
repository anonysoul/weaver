import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type GitCommandType = 'STATUS' | 'PULL' | 'CHECKOUT';

export interface GitCommandRequest {
  command: GitCommandType;
  branch?: string | null;
}

export interface GitCommandResponse {
  ok: boolean;
  command: GitCommandType;
  stdout: string;
  stderr: string;
  message: string;
}

export interface SessionLogResponse {
  id: number;
  message: string;
  createdAt: string;
}

export interface SessionContextResponse {
  sessionId: number;
  repoName: string;
  repoPathWithNamespace: string;
  workspacePath: string;
  status: string;
  defaultBranch?: string | null;
  currentBranch?: string | null;
  branches: string[];
  gitStatus: string[];
  directoryTree: string[];
  generatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class SessionRuntimeService {
  private readonly basePath = '/api';

  constructor(private readonly http: HttpClient) {}

  listLogs(
    sessionId: number,
    offset = 0,
    limit = 1000
  ): Observable<SessionLogResponse[]> {
    return this.http.get<SessionLogResponse[]>(
      `${this.basePath}/sessions/${sessionId}/logs`,
      { params: { offset, limit } }
    );
  }

  runGitCommand(sessionId: number, request: GitCommandRequest): Observable<GitCommandResponse> {
    return this.http.post<GitCommandResponse>(`${this.basePath}/sessions/${sessionId}/git`, request);
  }

  exportContext(sessionId: number): Observable<SessionContextResponse> {
    return this.http.get<SessionContextResponse>(`${this.basePath}/sessions/${sessionId}/context`);
  }
}
