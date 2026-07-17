import request from '@/utils/request'
import { encrypt } from '@/utils/rsaEncrypt'

export const LOGIN_PASSWORD_ENCRYPT_ERROR = 'LOGIN_PASSWORD_ENCRYPT_ERROR'

const legacyEncryptedPasswordPattern = /^[A-Za-z0-9+/]{86}==$/

function createPasswordEncryptError(password) {
  const message = legacyEncryptedPasswordPattern.test(password)
    ? '浏览器保存的是旧版加密密码，请重新输入原密码，并在登录成功后更新浏览器保存的密码'
    : '密码加密失败，请重新输入密码'
  const error = new Error(message)
  error.code = LOGIN_PASSWORD_ENCRYPT_ERROR
  return error
}

export function login(username, password, code, uuid) {
  const encryptedPassword = encrypt(password)
  if (!encryptedPassword) {
    return Promise.reject(createPasswordEncryptError(password))
  }
  return request({
    url: 'auth/login',
    method: 'post',
    data: {
      username,
      password: encryptedPassword,
      code,
      uuid
    }
  })
}

export function getInfo() {
  return request({
    url: 'auth/info',
    method: 'get'
  })
}

export function getCodeImg() {
  return request({
    url: 'auth/code',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: 'auth/logout',
    method: 'delete'
  })
}
