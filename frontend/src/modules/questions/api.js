import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const QUESTION_PAGE_PATHS = ['/questions/available', '/questions/pending-confirm']
const QUESTION_DEFAULT_API = '/api/questions'
const QUESTION_CATEGORY_API = '/api/question-categories'

function questionApiPath() {
  return resolveMenuApiPath(QUESTION_PAGE_PATHS, QUESTION_DEFAULT_API)
}

export function listCategories() {
  return api.get(QUESTION_CATEGORY_API)
}

export function createCategory(payload) {
  return api.post(QUESTION_CATEGORY_API, payload)
}

export function listQuestions(filters = {}) {
  const params = new URLSearchParams({
    page: String(filters.page || PAGE_DEFAULTS.page),
    size: String(filters.size || PAGE_DEFAULTS.size)
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
  return api.get(`${questionApiPath()}?${params.toString()}`)
}

export function getQuestion(id) {
  return api.get(`${questionApiPath()}/${id}`)
}

export function reviewQuestion(id, payload) {
  return api.post(`${questionApiPath()}/${id}/review`, payload)
}
