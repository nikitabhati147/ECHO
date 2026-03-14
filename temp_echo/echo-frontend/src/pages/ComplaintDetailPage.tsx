import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded'
import UpdateRoundedIcon from '@mui/icons-material/UpdateRounded'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { StatusChip } from '../components/StatusChip'
import type { Complaint, ComplaintStatus } from '../types'

const leaderStatuses: ComplaintStatus[] = ['NEW', 'IN_REVIEW', 'IN_PROGRESS', 'RESOLVED']

function formatTimestamp(timestamp: number) {
  return new Date(timestamp).toLocaleString()
}

export function ComplaintDetailPage() {
  const { complaintId } = useParams()
  const { token, user } = useAuth()
  const [complaint, setComplaint] = useState<Complaint | null>(null)
  const [status, setStatus] = useState<ComplaintStatus>('NEW')
  const [resolutionNote, setResolutionNote] = useState('')
  const [loading, setLoading] = useState(true)
  const [submittingStatus, setSubmittingStatus] = useState(false)
  const [submittingResolution, setSubmittingResolution] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const isLeader = user?.role === 'LEADER'

  const loadComplaint = useCallback(async () => {
    if (!token || !complaintId) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      const result = await api.getComplaint(complaintId, token)
      setComplaint(result)
      setStatus(result.status)
      setResolutionNote(result.resolutionNote ?? '')
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Unable to load complaint.')
    } finally {
      setLoading(false)
    }
  }, [complaintId, token])

  useEffect(() => {
    void loadComplaint()
  }, [loadComplaint])

  const canResolve = useMemo(
    () => isLeader && complaint && complaint.status !== 'RESOLVED',
    [complaint, isLeader],
  )

  async function handleStatusUpdate() {
    if (!token || !complaintId) {
      return
    }

    setSubmittingStatus(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await api.updateComplaintStatus(complaintId, status, token)
      setComplaint(updated)
      setSuccessMessage('Complaint status updated.')
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : 'Unable to update status.')
    } finally {
      setSubmittingStatus(false)
    }
  }

  async function handleResolve() {
    if (!token || !complaintId) {
      return
    }

    setSubmittingResolution(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const updated = await api.resolveComplaint(complaintId, resolutionNote, token)
      setComplaint(updated)
      setStatus(updated.status)
      setSuccessMessage('Complaint resolved successfully.')
    } catch (resolveError) {
      setError(resolveError instanceof Error ? resolveError.message : 'Unable to resolve complaint.')
    } finally {
      setSubmittingResolution(false)
    }
  }

  if (!complaintId || !user || !token) {
    return null
  }

  if (loading) {
    return (
      <Box sx={{ py: 8, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Box>
    )
  }

  if (!complaint) {
    return <Alert severity="error">{error ?? 'Complaint not found.'}</Alert>
  }

  return (
    <Stack spacing={3}>
      <Button component={RouterLink} to="/" startIcon={<ArrowBackRoundedIcon />} sx={{ alignSelf: 'flex-start' }}>
        Back to dashboard
      </Button>

      {error && <Alert severity="error">{error}</Alert>}
      {successMessage && <Alert severity="success">{successMessage}</Alert>}

      <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
        <CardContent>
          <Stack spacing={2}>
            <Stack
              direction={{ xs: 'column', md: 'row' }}
              justifyContent="space-between"
              spacing={1}
              alignItems={{ xs: 'flex-start', md: 'center' }}
            >
              <Box>
                <Typography variant="h4">{complaint.summary}</Typography>
                <Typography color="text.secondary">
                  Raised by {complaint.citizenName} on {formatTimestamp(complaint.createdAt)}
                </Typography>
              </Box>
              <StatusChip status={complaint.status} />
            </Stack>

            <Typography>{complaint.text}</Typography>

            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip label={complaint.category} />
              <Chip label={`${complaint.urgency} urgency`} color="warning" />
              <Chip label={`${complaint.suggestedPriority} priority`} color="primary" />
              {complaint.locationText && <Chip label={complaint.locationText} variant="outlined" />}
              {complaint.contactPhone && <Chip label={complaint.contactPhone} variant="outlined" />}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
        <CardContent>
          <Stack spacing={1.5}>
            <Typography variant="h6">AI assessment</Typography>
            <Typography color="text.secondary">{complaint.reasoning}</Typography>
          </Stack>
        </CardContent>
      </Card>

      {isLeader && (
        <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="h6">Leader actions</Typography>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <FormControl fullWidth>
                  <InputLabel id="status-select-label">Status</InputLabel>
                  <Select
                    labelId="status-select-label"
                    label="Status"
                    value={status}
                    onChange={(event) => setStatus(event.target.value as ComplaintStatus)}
                  >
                    {leaderStatuses.map((statusOption) => (
                      <MenuItem key={statusOption} value={statusOption}>
                        {statusOption.replaceAll('_', ' ')}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Button
                  variant="outlined"
                  startIcon={<UpdateRoundedIcon />}
                  onClick={() => void handleStatusUpdate()}
                  disabled={submittingStatus}
                >
                  {submittingStatus ? 'Updating...' : 'Update status'}
                </Button>
              </Stack>

              <TextField
                label="Resolution note"
                value={resolutionNote}
                onChange={(event) => setResolutionNote(event.target.value)}
                multiline
                minRows={3}
                fullWidth
              />
              <Button
                variant="contained"
                color="secondary"
                startIcon={<CheckCircleRoundedIcon />}
                onClick={() => void handleResolve()}
                disabled={!canResolve || submittingResolution}
              >
                {submittingResolution ? 'Resolving...' : 'Resolve complaint'}
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}

      <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Status history</Typography>
            {complaint.history.map((entry) => (
              <Box key={`${entry.changedAt}-${entry.status}`} sx={{ pb: 1.5, borderBottom: 1, borderColor: 'divider' }}>
                <Stack
                  direction={{ xs: 'column', md: 'row' }}
                  spacing={1}
                  justifyContent="space-between"
                  alignItems={{ xs: 'flex-start', md: 'center' }}
                >
                  <Stack direction="row" spacing={1} alignItems="center">
                    <StatusChip status={entry.status} />
                    <Typography fontWeight={600}>{entry.changedByName}</Typography>
                    <Typography color="text.secondary">({entry.changedByRole})</Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {formatTimestamp(entry.changedAt)}
                  </Typography>
                </Stack>
              </Box>
            ))}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  )
}
