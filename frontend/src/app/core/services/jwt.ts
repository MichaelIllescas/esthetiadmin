export interface JwtPayload {
  sub?: string;
  exp?: number;
}

/** Reads the claims of a JWT without verifying it (the backend is the one that validates it). */
export function readJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as JwtPayload;
  } catch {
    return null;
  }
}
