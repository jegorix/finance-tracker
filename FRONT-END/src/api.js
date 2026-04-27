const REQUEST_TIMEOUT_MS = 15000;
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

function joinFieldErrors(fieldErrors) {
  if (!Array.isArray(fieldErrors) || fieldErrors.length === 0) {
    return '';
  }

  return fieldErrors
    .map((item) => `${item.field}: ${item.message}`)
    .join('; ');
}

async function parseError(response) {
  try {
    const payload = await response.json();
    const fieldMessage = joinFieldErrors(payload.fieldErrors);
    return fieldMessage || payload.message || payload.error || `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

async function fetchWithTimeout(path, options = {}) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: options.headers || {},
      signal: controller.signal
    });
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error('Data is taking longer than expected to load. Please try again.');
    }

    if (error instanceof TypeError) {
      throw new Error('Unable to connect to the data service right now.');
    }

    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

async function requestRaw(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const response = await fetchWithTimeout(path, {
    ...options,
    headers
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return response;
}

async function request(path, options = {}) {
  const response = await requestRaw(path, options);

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

function toQuery(params) {
  const query = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') {
      return;
    }

    query.set(key, String(value));
  });

  const result = query.toString();
  return result ? `?${result}` : '';
}

export async function listUsers() {
  return request('/api/v1/users');
}

export async function createUser(payload) {
  return request('/api/v1/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateUser(id, payload) {
  return request(`/api/v1/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteUser(id) {
  return request(`/api/v1/users/${id}`, { method: 'DELETE' });
}

export async function listAccounts() {
  return request('/api/v1/accounts');
}

export async function createAccount(payload) {
  return request('/api/v1/accounts', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateAccount(id, payload) {
  return request(`/api/v1/accounts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteAccount(id) {
  return request(`/api/v1/accounts/${id}`, { method: 'DELETE' });
}

export async function listBudgets() {
  return request('/api/v1/budgets');
}

export async function createBudget(payload) {
  return request('/api/v1/budgets', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateBudget(id, payload) {
  return request(`/api/v1/budgets/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteBudget(id) {
  return request(`/api/v1/budgets/${id}`, { method: 'DELETE' });
}

export async function listCategories() {
  return request('/api/v1/categories');
}

export async function createCategory(payload) {
  return request('/api/v1/categories', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateCategory(id, payload) {
  return request(`/api/v1/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteCategory(id) {
  return request(`/api/v1/categories/${id}`, { method: 'DELETE' });
}

export async function listTransactions() {
  return request('/api/v1/transactions');
}

export async function createTransaction(payload) {
  return request('/api/v1/transactions', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateTransaction(id, payload) {
  return request(`/api/v1/transactions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteTransaction(id) {
  return request(`/api/v1/transactions/${id}`, { method: 'DELETE' });
}

export async function searchTransactions(filters) {
  const query = toQuery(filters);
  const response = await requestRaw(`/api/v1/transactions/search${query}`);
  const payload = await response.json();
  return {
    ...payload,
    source: response.headers.get('X-Transaction-Search-Source') || 'UNKNOWN'
  };
}
