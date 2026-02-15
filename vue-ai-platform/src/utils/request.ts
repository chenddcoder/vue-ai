import axios from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 120000 // AI API需要更长的超时时间，120秒
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200 || res.code === 1) {
      return res
    } else if (res.code === 404) {
      return res
    } else {
      console.error('API Error:', res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    console.error('Network Error:', error)
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请稍后重试或检查AI配置'))
    }
    return Promise.reject(error)
  }
)

export default service
