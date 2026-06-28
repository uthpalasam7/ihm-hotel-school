export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  code?: string;
  message?: string;
  fieldErrors?: ApiFieldError[];
}

export function errorMessage(error: unknown, fallback = 'The request could not be completed'): string {
	const response = error as { error?: ApiError };
	return response.error?.message ?? fallback;
}

export function fieldError(error: unknown, field: string): string | null {
	if (!error) {
		return null;
	}
	const response = error as { error?: ApiError };
	return response.error?.fieldErrors?.find((item) => item.field === field)?.message ?? null;
}
