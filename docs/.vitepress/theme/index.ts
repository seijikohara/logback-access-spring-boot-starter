import type { Theme } from 'vitepress'
import DefaultTheme from 'vitepress/theme'
import Mermaid from './Mermaid.vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('Mermaid', Mermaid)
  },
} satisfies Theme
