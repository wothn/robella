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
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('isRefreshing')
  },

  /**
   * 获取访问令牌
   */
  getAccessToken(): string | null {
    return this.getItem('accessToken')
  },

  /**
   * 获取刷新令牌
   */
  getRefreshToken(): string | null {
    return this.getItem('refreshToken')
  },

  /**
   * 设置认证令牌
   */
  setAuthTokens(accessToken: string, refreshToken: string): void {
    this.setItem('accessToken', accessToken)
    this.setItem('refreshToken', refreshToken)
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