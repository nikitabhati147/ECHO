import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded'
import SendRoundedIcon from '@mui/icons-material/SendRounded'
import { Alert, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import { api } from '../api/client'
import type { Complaint, ComplaintAnalysis } from '../types'

export function ComplaintForm({
  token,
  onCreated,
}: {
  token: string
  onCreated: (complaint: Complaint) => void
}) {
  const [text, setText] = useState('')
  const [locationText, setLocationText] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [analysis, setAnalysis] = useState<ComplaintAnalysis | null>(null)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handlePreviewAnalysis() {
    if (!text.trim()) {
      setError('Enter a complaint before requesting AI analysis.')
      return
    }

    setError(null)
    setLoadingAnalysis(true)
    try {
      const result = await api.analyzeComplaint(text)
      setAnalysis(result)
    } catch (analysisError) {
      setError(analysisError instanceof Error ? analysisError.message : 'AI analysis failed.')
    } finally {
      setLoadingAnalysis(false)
    }
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const complaint = await api.createComplaint(
        {
          text,
          locationText: locationText || undefined,
          contactPhone: contactPhone || undefined,
        },
        token,
      )
      setText('')
      setLocationText('')
      setContactPhone('')
      setAnalysis(null)
      onCreated(complaint)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Complaint submission failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card elevation={0} sx={{ border: 1, borderColor: 'divider' }}>
      <CardContent>
        <Stack component="form" spacing={2} onSubmit={handleSubmit}>
          <Typography variant="h6">Submit a complaint</Typography>
          <Typography variant="body2" color="text.secondary">
            Describe the issue in plain language. The backend will classify and prioritize it with Gemini.
          </Typography>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Complaint details"
            value={text}
            onChange={(event) => setText(event.target.value)}
            multiline
            minRows={4}
            required
            fullWidth
          />
          <TextField
            label="Location"
            value={locationText}
            onChange={(event) => setLocationText(event.target.value)}
            placeholder="Village road near the water tank"
            fullWidth
          />
          <TextField
            label="Contact phone"
            value={contactPhone}
            onChange={(event) => setContactPhone(event.target.value)}
            fullWidth
          />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
            <Button
              variant="outlined"
              startIcon={<AutoAwesomeRoundedIcon />}
              onClick={handlePreviewAnalysis}
              disabled={loadingAnalysis}
            >
              {loadingAnalysis ? 'Analyzing...' : 'Preview AI analysis'}
            </Button>
            <Button type="submit" variant="contained" startIcon={<SendRoundedIcon />} disabled={submitting}>
              {submitting ? 'Submitting...' : 'Submit complaint'}
            </Button>
          </Stack>
          {analysis && (
            <Alert severity="info">
              <strong>{analysis.category}</strong> | {analysis.urgency} urgency | {analysis.suggestedPriority}{' '}
              priority
              <br />
              {analysis.summary}
            </Alert>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}
