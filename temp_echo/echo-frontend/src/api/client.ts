import type { ApiError, AuthResponse, Complaint, ComplaintAnalysis, ComplaintStatus, User } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

interface RequestOptions extends RequestInit {
  token?: string | null
}

async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { token, headers, ...rest } = options

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  })

  if (!response.ok) {
    const fallbackMessage = `Request failed with status ${response.status}`
    try {
      const body = (await response.json()) as ApiError
      throw new Error(body.message || fallbackMessage)
    } catch (error) {
      if (error instanceof Error && error.message !== 'Unexpected end of JSON input') {
        throw error
      }
      throw new Error(fallbackMessage)
    }
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const api = {
  signup: (payload: { name: string; email: string; password: string }) =>
    apiRequest<AuthResponse>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  login: (payload: { email: string; password: string }) =>
    apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  getCurrentUser: (token: string) =>
    apiRequest<User>('/auth/me', {
      token,
    }),

  analyzeComplaint: (text: string) =>
    apiRequest<ComplaintAnalysis>('/ai/complaints/analyze', {
      method: 'POST',
      body: JSON.stringify({ text }),
    }),

  createComplaint: (
    payload: { text: string; locationText?: string; contactPhone?: string },
    token: string,
  ) =>
    apiRequest<Complaint>('/complaints', {
      method: 'POST',
      token,
      body: JSON.stringify(payload),
    }),

  listComplaints: (token: string) =>
    apiRequest<Complaint[]>('/complaints', {
      token,
    }),

  getComplaint: (complaintId: string, token: string) =>
    apiRequest<Complaint>(`/complaints/${complaintId}`, {
      token,
    }),

  updateComplaintStatus: (complaintId: string, status: ComplaintStatus, token: string) =>
    apiRequest<Complaint>(`/complaints/${complaintId}/status`, {
      method: 'PATCH',
      token,
      body: JSON.stringify({ status }),
    }),

  resolveComplaint: (complaintId: string, resolutionNote: string, token: string) =>
    apiRequest<Complaint>(`/complaints/${complaintId}/resolve`, {
      method: 'PATCH',
      token,
      body: JSON.stringify({ resolutionNote }),
    }),
}
