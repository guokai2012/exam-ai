import { api } from '../../api/http'

export function listSystemConfigs() {
  return api.get('/api/system-configs')
}

export function updateSystemConfig(key, configValue) {
  return api.putResult(`/api/system-configs/${key}`, { configValue: String(configValue ?? '') })
}
