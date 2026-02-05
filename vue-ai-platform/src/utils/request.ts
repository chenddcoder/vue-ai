import axios from 'axios'

const service = axios.create({
  baseURL: '/',
  timeout: 5000
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
    if (res.code !== 200 && res.code !== 1) { // 1 is for AI controller compatibility
        // Handle error
        console.error('API Error:', res.message)
        return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  error => {
    console.error('Network Error:', error)
    return Promise.reject(error)
  }
)

export default service
