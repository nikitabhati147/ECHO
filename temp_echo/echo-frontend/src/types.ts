export type UserRole = 'CITIZEN' | 'LEADER'

export type ComplaintStatus = 'NEW' | 'IN_REVIEW' | 'IN_PROGRESS' | 'RESOLVED'

export interface User {
  id: string
  name: string
  email: string
  role: UserRole
  createdAt: number
}

export interface AuthResponse {
  token: string
  user: User
}

export interface ComplaintStatusHistory {
  status: ComplaintStatus
  changedByUserId: string
  changedByName: string
  changedByRole: UserRole
  changedAt: number
}

export interface Complaint {
  id: string
  citizenId: string
  citizenName: string
  text: string
  locationText?: string
  contactPhone?: string
  category: string
  urgency: string
  suggestedPriority: string
  summary: string
  reasoning: string
  status: ComplaintStatus
  resolutionNote?: string
  createdAt: number
  updatedAt: number
  history: ComplaintStatusHistory[]
}

export interface ComplaintAnalysis {
  category: string
  urgency: string
  suggestedPriority: string
  summary: string
  reasoning: string
}

export interface ApiError {
  message: string
}
