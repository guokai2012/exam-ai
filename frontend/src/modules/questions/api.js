import { api } from '../../api/http'

export function listCategories() {
  return api.get('/api/question-categories')
}

export function createCategory(payload) {
  return api.post('/api/question-categories', payload)
}

export function listQuestions(filters = {}) {
  const params = new URLSearchParams({
    page: String(filters.page || 1),
    size: String(filters.size || 20)
  })
  if (filters.categoryId) {
    params.set('categoryId', filters.categoryId)
  }
  if (filters.state) {
    params.set('state', filters.state)
  }
  if (filters.questionType) {
    params.set('questionType', filters.questionType)
  }
  if (filters.tagId) {
    params.set('tagId', filters.tagId)
  }
  return api.get(`/api/questions?${params.toString()}`)
}

export function getQuestion(id) {
  return api.get(`/api/questions/${id}`)
}

export function reviewQuestion(id, payload) {
  return api.post(`/api/questions/${id}/review`, payload)
}
