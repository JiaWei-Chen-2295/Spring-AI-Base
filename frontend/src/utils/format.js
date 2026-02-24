export function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export function truncate(s, max = 120) {
  if (!s) return '';
  return s.length <= max ? s : s.slice(0, max) + '...';
}
