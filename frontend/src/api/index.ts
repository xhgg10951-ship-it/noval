import axios from 'axios'

// Central Axios instance. All frontend calls go to /api/* which the Vite dev
// server proxies to the Spring Boot backend (port 8080). The frontend never
// calls the Python AI Service directly.
const api = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

export default api
