import { api } from '../../api/http'
import { PAGE_DEFAULTS } from '../../shared/constants'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const DOCUMENTS_PAGE_PATH = '/documents'
const DOCUMENTS_DEFAULT_API = '/api/documents'

function documentApiPath() {
  return resolveMenuApiPath(DOCUMENTS_PAGE_PATH, DOCUMENTS_DEFAULT_API)
}

export function listDocuments(page = PAGE_DEFAULTS.page, size = PAGE_DEFAULTS.size) {
  return api.get(`${documentApiPath()}?page=${page}&size=${size}`)
}

export function uploadDocument(file) {
  const body = new FormData()
  body.append('file', file)
  return api.upload(documentApiPath(), body)
}

export function getDocumentContent(id) {
  return api.get(`${documentApiPath()}/${id}/content`)
}

export function getDocumentDetail(id) {
  return api.get(`${documentApiPath()}/${id}`)
}

export function analyzeDocument(id) {
  return api.post(`${documentApiPath()}/${id}/analysis`)
}

export function latestAnalysis(id) {
  return api.get(`${documentApiPath()}/${id}/analysis/latest`)
}
