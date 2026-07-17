/* eslint-env jest */
import request from '@/utils/request'
import { encrypt } from '@/utils/rsaEncrypt'
import { login, LOGIN_PASSWORD_ENCRYPT_ERROR } from '@/api/login'

jest.mock('@/utils/request', () => jest.fn(() => Promise.resolve({})))
jest.mock('@/utils/rsaEncrypt', () => ({
  encrypt: jest.fn()
}))

describe('login api', () => {
  beforeEach(() => {
    request.mockClear()
    encrypt.mockReset()
  })

  it('encrypts the password at the api boundary without changing the source value', () => {
    const password = 'plain-password'
    encrypt.mockReturnValue('encrypted-password')

    login('admin', password, '1234', 'uuid')

    expect(encrypt).toHaveBeenCalledWith(password)
    expect(request).toHaveBeenCalledWith({
      url: 'auth/login',
      method: 'post',
      data: {
        username: 'admin',
        password: 'encrypted-password',
        code: '1234',
        uuid: 'uuid'
      }
    })
    expect(password).toBe('plain-password')
  })

  it('rejects a legacy encrypted password without sending a request', async() => {
    encrypt.mockReturnValue(false)
    const legacyEncryptedPassword = 'A'.repeat(86) + '=='

    await expect(login('admin', legacyEncryptedPassword, '1234', 'uuid')).rejects.toMatchObject({
      code: LOGIN_PASSWORD_ENCRYPT_ERROR,
      message: '浏览器保存的是旧版加密密码，请重新输入原密码，并在登录成功后更新浏览器保存的密码'
    })
    expect(request).not.toHaveBeenCalled()
  })
})
