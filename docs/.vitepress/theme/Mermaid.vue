<script lang="ts">
// Shared by all instances: every mermaid.render() call needs an ID that no other element on the page has.
// Mermaid first removes any element with that ID, then draws into a temporary SVG that it finds again by ID.
// A reused ID can therefore remove a displayed diagram, or draw into it and return an empty SVG.
let renderCount = 0
</script>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useData } from 'vitepress'

const props = defineProps<{
  // URI-encoded so that Vue does not interpret diagram text as template syntax.
  graph: string
}>()

const { isDark } = useData()
const svg = ref('')
const error = ref('')
let latestRender = 0

const render = async () => {
  const renderId = ++renderCount
  latestRender = renderId
  // Import lazily: mermaid needs the DOM, and a static import would also pull it into the main bundle.
  const { default: mermaid } = await import('mermaid')
  mermaid.initialize({
    startOnLoad: false,
    theme: isDark.value ? 'dark' : 'neutral',
    themeVariables: {
      primaryColor: '#3b82f6',
      primaryTextColor: '#1e293b',
      primaryBorderColor: '#60a5fa',
      lineColor: '#64748b',
      secondaryColor: '#f1f5f9',
      tertiaryColor: '#e2e8f0',
    },
    // Mermaid 12 defaults to the ELK layout and the neo look; keep the pre-12 layout and look.
    layout: 'dagre',
    look: 'classic',
    // Without this, a failed render leaves Mermaid's error SVG in document.body,
    // and the next render uses a new ID, so nothing ever removes it.
    suppressErrorRendering: true,
  })
  // A theme toggle or a hot update can start a newer render before this one finishes.
  // Mermaid applies the configuration that is current when a queued render runs,
  // so only the newest render is guaranteed to match the current theme and source.
  try {
    const result = await mermaid.render(`mermaid-${renderId}`, decodeURIComponent(props.graph))
    if (renderId === latestRender) {
      svg.value = result.svg
      error.value = ''
    }
  } catch (cause) {
    if (renderId === latestRender) {
      svg.value = ''
      error.value = cause instanceof Error ? cause.message : String(cause)
    }
  }
}

onMounted(render)
// Re-render when the parent updates `graph` in place; do not rely on a remount.
watch([isDark, () => props.graph], render)
</script>

<template>
  <pre v-if="error" class="mermaid-error">{{ error }}</pre>
  <div v-else class="mermaid" v-html="svg"></div>
</template>
