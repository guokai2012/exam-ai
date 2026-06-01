import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'

export function listDocuments(page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  return api.get(`/api/documents?page=${page}&size=${size}`)
}

export function uploadDocument(file) {
  const body = new FormData()
  body.append('file', file)
  return api.upload('/api/documents', body)
}

export function getDocumentContent(id) {
  return api.get(`/api/documents/${id}/content`)
}

export function getDocumentDetail(id) {
  return api.get(`/api/documents/${id}`)
}

export function analyzeDocument(id) {
  return api.post(`/api/documents/${id}/analysis`)
}

export function latestAnalysis(id) {
  return api.get(`/api/documents/${id}/analysis/latest`)
}
