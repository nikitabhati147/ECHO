import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Stack,
  Typography,
} from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { ComplaintForm } from '../components/ComplaintForm'
import { StatusChip } from '../components/StatusChip'
import type { Complaint } from '../types'

function formatTimestamp(timestamp: number) {
  return new Date(timestamp).toLocaleString()
}

export function DashboardPage() {
  const { token, user } = useAuth()
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadComplaints = useCallback(async () => {
    if (!token) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      const result = await api.listComplaints(token)
      setComplaints(result)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Unable to load complaints.')
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    void loadComplaints()
  }, [loadComplaints])

  const stats = useMemo(
    () => ({
      total: complaints.length,
      open: complaints.filter((complaint) => complaint.status !== 'RESOLVED').length,
      resolved: complaints.filter((complaint) => complaint.status === 'RESOLVED').length,
    }),
    [complaints],
  )

  if (!user || !token) {
    return null
  }

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        justifyContent="space-between"
        spacing={2}
        alignItems={{ xs: 'flex-start', md: 'center' }}
      >
        <Box>
          <Typography variant="h4" gutterBottom>
            {user.role === 'LEADER' ? 'Leader dashboard' : 'Citizen dashboard'}
          </Typography>
          <Typography color="text.secondary">
            {user.role === 'LEADER'
              ? 'Review AI-prioritized grievances and update resolution status.'
              : 'Submit issues, preview the AI analysis, and track progress.'}
          </Typography>
        </Box>
        <Button variant="outlined" startIcon={<RefreshRoundedIcon />} onClick={() => void loadComplaints()}>
          Refresh
        </Button>
      </Stack>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Total complaints
              </Typography>
              <Typography variant="h4">{stats.total}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Open complaints
              </Typography>
              <Typography variant="h4">{stats.open}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
            <CardContent>
              <Typography color="text.secondary" variant="body2">
                Resolved complaints
              </Typography>
              <Typography variant="h4">{stats.resolved}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {user.role === 'CITIZEN' && (
        <ComplaintForm token={token} onCreated={(complaint) => setComplaints((current) => [complaint, ...current])} />
      )}

      {error && <Alert severity="error">{error}</Alert>}

      <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
        <CardContent>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            justifyContent="space-between"
            spacing={1}
            sx={{ mb: 2 }}
          >
            <Typography variant="h6">
              {user.role === 'LEADER' ? 'All complaints' : 'My complaints'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Sorted newest first
            </Typography>
          </Stack>

          {loading ? (
            <Box sx={{ py: 6, display: 'flex', justifyContent: 'center' }}>
              <CircularProgress />
            </Box>
          ) : complaints.length === 0 ? (
            <Alert severity="info">No complaints yet. Submit one to start the workflow.</Alert>
          ) : (
            <Stack spacing={2}>
              {complaints.map((complaint) => (
                <Card key={complaint.id} variant="outlined">
                  <CardContent>
                    <Stack spacing={1.5}>
                      <Stack
                        direction={{ xs: 'column', md: 'row' }}
                        justifyContent="space-between"
                        spacing={1}
                        alignItems={{ xs: 'flex-start', md: 'center' }}
                      >
                        <Typography variant="h6">{complaint.summary}</Typography>
                        <StatusChip status={complaint.status} />
                      </Stack>
                      <Typography color="text.secondary">{complaint.text}</Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                        <Chip label={complaint.category} size="small" />
                        <Chip label={`${complaint.urgency} urgency`} size="small" color="warning" />
                        <Chip label={`${complaint.suggestedPriority} priority`} size="small" color="primary" />
                        {complaint.locationText && <Chip label={complaint.locationText} size="small" variant="outlined" />}
                      </Stack>
                      <Stack
                        direction={{ xs: 'column', md: 'row' }}
                        justifyContent="space-between"
                        spacing={1}
                        alignItems={{ xs: 'flex-start', md: 'center' }}
                      >
                        <Typography variant="body2" color="text.secondary">
                          Raised by {complaint.citizenName} on {formatTimestamp(complaint.createdAt)}
                        </Typography>
                        <Button
                          component={RouterLink}
                          to={`/complaints/${complaint.id}`}
                          endIcon={<VisibilityRoundedIcon />}
                        >
                          View details
                        </Button>
                      </Stack>
                    </Stack>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>
    </Stack>
  )
}
