import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import { AppBar, Avatar, Box, Button, Chip, Container, Stack, Toolbar, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" elevation={0} color="inherit" sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar>
          <Typography
            component={RouterLink}
            to="/"
            variant="h6"
            sx={{ color: 'primary.main', fontWeight: 800, letterSpacing: 0.4 }}
          >
            ECHO
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          {user && (
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Chip
                label={user.role === 'LEADER' ? 'Leader' : 'Citizen'}
                color={user.role === 'LEADER' ? 'secondary' : 'primary'}
                size="small"
              />
              <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                {user.name.charAt(0).toUpperCase()}
              </Avatar>
              <Box sx={{ display: { xs: 'none', md: 'block' } }}>
                <Typography variant="body2" fontWeight={700}>
                  {user.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {user.email}
                </Typography>
              </Box>
              <Button color="inherit" startIcon={<LogoutRoundedIcon />} onClick={logout}>
                Logout
              </Button>
            </Stack>
          )}
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ py: 4 }}>
        {children}
      </Container>
    </Box>
  )
}
