/**
 * Cookie 工具函数
 */
export const cookies = {
  /**
   * 设置 Cookie
   */
  setItem(name: string, value: string, days: number = 7): void {
    const date = new Date()
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000))
    const expires = `expires=${date.toUTCString()}`
    document.cookie = `${name}=${value};${expires};path=/`
  },

  /**
   * 获取 Cookie
   */
  getItem(name: string): string | null {
    const nameEQ = `${name}=`
    const ca = document.cookie.split(';')
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i]
      while (c.charAt(0) === ' ') c = c.substring(1, c.length)
      if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length)
    }
    return null
  },

  /**
   * 删除 Cookie
   */
  removeItem(name: string): void {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`
  }
}

/**
 * localStorage 工具函数
 */
export const storage = {
  /**
   * 设置存储项
   */
  setItem(key: string, value: string): void {
    localStorage.setItem(key, value)
  },

  /**
   * 获取存储项
   */
  getItem(key: string): string | null {
    return localStorage.getItem(key)
  },

  /**
   * 清除所有认证相关的存储项
   */
  clearAuth(): void {
    localStorage.removeItem('isRefreshing')
  },

  /**
   * 获取访问令牌
   */
  getAccessToken(): string | null {
    return null
  },



  /**
   * 检查是否正在刷新令牌
   */
  isRefreshing(): boolean {
    return this.getItem('isRefreshing') === 'true'
  },

  /**
   * 设置刷新状态
   */
  setRefreshing(isRefreshing: boolean): void {
    this.setItem('isRefreshing', isRefreshing ? 'true' : 'false')
  }
}