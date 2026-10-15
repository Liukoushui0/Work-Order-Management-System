import { config } from '@vue/test-utils'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 测试全局设置：与 main.js 保持一致，注册 Element Plus 图标组件，
 * 避免每个用例单独处理后出现 "Failed to resolve component: Plus/User/Lock" 警告。
 */
config.global.components = { ...ElementPlusIconsVue }
