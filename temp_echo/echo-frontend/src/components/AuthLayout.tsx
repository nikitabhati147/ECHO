import { Box, Paper, Stack, Typography } from '@mui/material'

export function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle: string
  children: React.ReactNode
}) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 2,
        py: 4,
        background:
          'linear-gradient(180deg, rgba(30,77,183,0.10) 0%, rgba(244,247,251,1) 45%, rgba(244,247,251,1) 100%)',
      }}
    >
      <Paper elevation={0} sx={{ width: '100%', maxWidth: 480, p: 4, border: 1, borderColor: 'divider' }}>
        <Stack spacing={1} sx={{ mb: 3 }}>
          <Typography variant="overline" color="primary.main" fontWeight={800}>
            ECHO
          </Typography>
          <Typography variant="h4">{title}</Typography>
          <Typography color="text.secondary">{subtitle}</Typography>
        </Stack>
        {children}
      </Paper>
    </Box>
  )
}
