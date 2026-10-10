/* eslint-env jest */
import IouUser from '@/views/iou/iouUser/index.vue'
import { revealIdentity } from '@/api/iouUser'

jest.mock('@/api/iouUser', () => ({ revealIdentity: jest.fn() }))
jest.mock('@crud/crud', () => ({
  __esModule: true,
  default: { HOOK: { beforeRefresh: 'beforeCrudRefresh' }},
  presenter: () => ({}), header: () => ({}), crud: () => ({})
}), { virtual: true })
jest.mock('@crud/RR.operation', () => ({}), { virtual: true })
jest.mock('@crud/CRUD.operation', () => ({}), { virtual: true })
jest.mock('@crud/Pagination', () => ({}), { virtual: true })
jest.mock('@/components/DateRangePicker', () => ({}))

function createVm() {
  return {
    ...IouUser.data(),
    ...IouUser.methods,
    $set: (object, key, value) => { object[key] = value },
    $delete: (object, key) => { delete object[key] }
  }
}

describe('identity visibility lifecycle', () => {
  beforeEach(() => revealIdentity.mockReset())

  it('reveals only the selected row and allows manual hiding', async() => {
    const vm = createVm()
    revealIdentity.mockResolvedValue({ idNo: '000000200001010001' })
    await vm.revealIdentity({ id: '1' })
    expect(vm.identityValues).toEqual({ '1': '000000200001010001' })
    vm.hideIdentity('1')
    expect(vm.identityValues).toEqual({})
  })

  it.each(['beforeCrudRefresh', 'deactivated', 'beforeDestroy', 'beforeRouteLeave'])('clears plaintext and ignores pending responses on %s', async(hook) => {
    const vm = createVm()
    let complete
    revealIdentity.mockReturnValue(new Promise(resolve => { complete = resolve }))
    vm.identityValues = { '2': '000000200001010002' }
    const pending = vm.revealIdentity({ id: '1' })
    const lifecycle = IouUser[hook] || IouUser.methods[hook]
    lifecycle.call(vm, {}, {}, jest.fn())
    expect(vm.identityValues).toEqual({})
    complete({ idNo: '000000200001010001' })
    await pending
    expect(vm.identityValues).toEqual({})
    expect(vm.identityLoading).toEqual({})
  })

  it('suppresses duplicate clicks and leaves the mask on a failed request', async() => {
    const vm = createVm()
    let fail
    revealIdentity.mockReturnValue(new Promise((resolve, reject) => { fail = reject }))
    const pending = vm.revealIdentity({ id: '1' })
    await vm.revealIdentity({ id: '1' })
    expect(revealIdentity).toHaveBeenCalledTimes(1)
    fail(new Error('request failed'))
    await pending
    expect(vm.identityValues).toEqual({})
    expect(vm.identityLoading).toEqual({})
  })
})
