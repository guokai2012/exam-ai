import { api } from '../../api/http'

export function listCategories() {
  return api.get('/api/question-categories')
}

export function listQuestions(filters = {}) {
  const params = new URLSearchParams({ page: '1', size: '20' })
  if (filters.categoryId) {
    params.set('categoryId', filters.categoryId)
  }
  if (filters.state) {
    params.set('state', filters.state)
  }
  return api.get(`/api/questions?${params.toString()}`)
}

export function reviewQuestion(id, payload) {
  return api.post(`/api/questions/${id}/review`, payload)
}
