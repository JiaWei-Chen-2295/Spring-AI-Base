import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Dashboard from './pages/Dashboard';
import Chat from './pages/Chat';
import Models from './pages/Models';
import Tools from './pages/Tools';
import Skills from './pages/Skills';
import Settings from './pages/Settings';
import Login from './pages/Login';
import Users from './pages/Users';
import Roles from './pages/Roles';
import { useAuthStore } from './core/state/authStore';

// Auth Guard Component
function AuthGuard({ children, requireAdmin = false }) {
  const { isAuthenticated, authRequired, isAdmin } = useAuthStore();

  // When auth is not required, let everyone through
  if (!authRequired) {
    return children;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requireAdmin && !isAdmin()) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

// Public Route - redirect to dashboard if authenticated (or auth disabled)
function PublicRoute({ children }) {
  const { isAuthenticated, authRequired } = useAuthStore();

  if (!authRequired || isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

export default function App() {
  const checkAuthRequired = useAuthStore((s) => s.checkAuthRequired);

  useEffect(() => {
    checkAuthRequired();
  }, [checkAuthRequired]);

  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <AuthGuard>
              <MainLayout />
            </AuthGuard>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="chat" element={<Chat />} />
          <Route path="models" element={<Models />} />
          <Route path="tools" element={<Tools />} />
          <Route path="skills" element={<Skills />} />
          <Route path="settings" element={<Settings />} />

          {/* Admin Only Routes */}
          <Route
            path="users"
            element={
              <AuthGuard requireAdmin>
                <Users />
              </AuthGuard>
            }
          />
          <Route
            path="roles"
            element={
              <AuthGuard requireAdmin>
                <Roles />
              </AuthGuard>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
