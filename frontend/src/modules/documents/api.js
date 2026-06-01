import { api } from '../../api/http'

export function listDocuments(page = 1, size = 20) {
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

export function analyzeDocument(id) {
  return api.post(`/api/documents/${id}/analysis`)
}

export function latestAnalysis(id) {
  return api.get(`/api/documents/${id}/analysis/latest`)
}
