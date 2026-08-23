import axios from 'axios'

// Central Axios instance. All frontend calls go to /api/* which the Vite dev
// server proxies to the Spring Boot backend (port 8080). The frontend never
// calls the Python AI Service directly.
//
// NOTE: with the release LLM (qwen3.7-plus) a single chapter generation can take 30s+.
// The previous 10s timeout made the browser abort the request and surface a
// false "请求失败，请检查后端服务与 AI 服务是否运行". Raise it well above
// worst-case generation time.
const api = axios.create({
  baseURL: '/api',
  timeout: 300_000,
})

export default api
