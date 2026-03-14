import { Chip } from '@mui/material'
import type { ComplaintStatus } from '../types'

const statusColorMap: Record<ComplaintStatus, 'default' | 'info' | 'warning' | 'success'> = {
  NEW: 'default',
  IN_REVIEW: 'info',
  IN_PROGRESS: 'warning',
  RESOLVED: 'success',
}

export function StatusChip({ status }: { status: ComplaintStatus }) {
  return <Chip label={status.replaceAll('_', ' ')} color={statusColorMap[status]} size="small" />
}
