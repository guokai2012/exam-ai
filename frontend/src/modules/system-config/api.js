import { api } from '../../api/http'
import { resolveMenuApiPath } from '../../shared/menuApiPath'

const SYSTEM_CONFIG_PAGE_PATH = '/system-configs'
const SYSTEM_CONFIG_DEFAULT_API = '/api/system-configs'

function systemConfigApiPath() {
  return resolveMenuApiPath(SYSTEM_CONFIG_PAGE_PATH, SYSTEM_CONFIG_DEFAULT_API)
}

export function listSystemConfigs() {
  return api.get(systemConfigApiPath())
}

export function updateSystemConfig(key, configValue) {
  return api.putResult(`${systemConfigApiPath()}/${key}`, { configValue: String(configValue ?? '') })
}
