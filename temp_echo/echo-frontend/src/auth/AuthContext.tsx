import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import type { AuthResponse, User } from '../types'
import { AuthContext, type AuthContextValue } from './context'

const TOKEN_STORAGE_KEY = 'echo-auth-token'

function storeAuth(authResponse: AuthResponse) {
  localStorage.setItem(TOKEN_STORAGE_KEY, authResponse.token)
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY))
  const [user, setUser] = useState<User | null>(null)
  const [isBootstrapping, setIsBootstrapping] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    if (!token) {
      setUser(null)
      return
    }

    try {
      const currentUser = await api.getCurrentUser(token)
      setUser(currentUser)
    } catch {
      logout()
      throw new Error('Session expired. Please log in again.')
    }
  }, [logout, token])

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      if (!token) {
        setIsBootstrapping(false)
        return
      }

      try {
        const currentUser = await api.getCurrentUser(token)
        if (!cancelled) {
          setUser(currentUser)
        }
      } catch {
        if (!cancelled) {
          logout()
        }
      } finally {
        if (!cancelled) {
          setIsBootstrapping(false)
        }
      }
    }

    bootstrap()

    return () => {
      cancelled = true
    }
  }, [logout, token])

  const login = useCallback(async (email: string, password: string) => {
    const authResponse = await api.login({ email, password })
    storeAuth(authResponse)
    setToken(authResponse.token)
    setUser(authResponse.user)
  }, [])

  const signup = useCallback(async (name: string, email: string, password: string) => {
    const authResponse = await api.signup({ name, email, password })
    storeAuth(authResponse)
    setToken(authResponse.token)
    setUser(authResponse.user)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      isBootstrapping,
      login,
      signup,
      logout,
      refreshUser,
    }),
    [token, user, isBootstrapping, login, signup, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
