export function getAvatarBorderStyle(borderCode?: string | null): string {
  switch (borderCode) {
    case 'BORDER_BRONZE':
      return '3px solid #cd7f32';
    case 'BORDER_SILVER':
      return '3px solid #94a3b8';
    case 'BORDER_GOLD':
      return '3px solid #eab308';
    case 'BORDER_EMERALD':
      return '3px solid #10b981';
    case 'BORDER_DIAMOND':
      return '3px solid #06b6d4';
    case 'BORDER_AURORA':
      return '3px solid #8b5cf6';
    default:
      return '3px solid #e2e8f0';
  }
}
