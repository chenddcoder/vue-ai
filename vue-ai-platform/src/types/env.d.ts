/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface Window {
  MonacoEnvironment: any
}

declare module 'monaco-editor/esm/vs/editor/editor.worker?worker' {
  const worker: new () => Worker
  export default worker
}

declare module 'monaco-editor/esm/vs/language/json/json.worker?worker' {
  const worker: new () => Worker
  export default worker
}

declare module 'monaco-editor/esm/vs/language/css/css.worker?worker' {
  const worker: new () => Worker
  export default worker
}

declare module 'monaco-editor/esm/vs/language/html/html.worker?worker' {
  const worker: new () => Worker
  export default worker
}

declare module 'monaco-editor/esm/vs/language/typescript/ts.worker?worker' {
  const worker: new () => Worker
  export default worker
}
