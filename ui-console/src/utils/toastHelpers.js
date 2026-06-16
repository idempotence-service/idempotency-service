export function toastClass(type) {
  return {
    success: 'bg-emerald-900/90 border-emerald-700 text-emerald-100',
    error: 'bg-red-900/90 border-red-700 text-red-100',
    info: 'bg-slate-800/90 border-slate-700 text-slate-100',
  }[type] ?? 'bg-slate-800/90 border-slate-700 text-slate-100'
}

export function toastIcon(type) {
  return { success: '✓', error: '✕', info: 'ℹ' }[type] ?? 'ℹ'
}
