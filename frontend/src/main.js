import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { applyTheme } from './modules/settings/themeSettings'
import './styles.css'

applyTheme()

createApp(App).use(router).mount('#app')
