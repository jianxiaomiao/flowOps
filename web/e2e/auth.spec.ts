import { expect, test, type Page } from '@playwright/test'

const apiBaseURL = process.env.FLOWOPS_E2E_API_BASE_URL ?? 'http://127.0.0.1:8080/flowOps'
const tenantCode = process.env.FLOWOPS_E2E_TENANT_CODE
const username = process.env.FLOWOPS_E2E_USERNAME
const password = process.env.FLOWOPS_E2E_PASSWORD
const providedToken = process.env.FLOWOPS_E2E_TOKEN

test('未登录访问工作台会跳转到登录页', async ({ page }) => {
  await page.goto('/tickets')

  await expect(page).toHaveURL(/\/login\?redirect=\/tickets$/)
  await expect(page.getByRole('heading', { name: '登录工作台' })).toBeVisible()
})

test('错误凭据会显示统一认证失败提示', async ({ page }) => {
  const suffix = Date.now().toString()
  await page.goto('/login')
  await page.getByLabel('企业编码').fill(`E2E_${suffix}`)
  await page.getByLabel('用户名').fill(`unknown_${suffix}`)
  await page.getByLabel('密码').fill('WrongPassword@123')
  await page.getByRole('button', { name: /进入 FlowOps/ }).click()

  await expect(page.locator('.el-message')).toContainText('租户、用户名或密码不正确')
  await expect(page).toHaveURL(/\/login$/)
})

test('真实登录态可以读取未读数，并在退出后撤销原 Token', async ({ page, request }) => {
  test.skip(!providedToken && !(tenantCode && username && password),
    '设置 FLOWOPS_E2E_TOKEN，或设置 FLOWOPS_E2E_TENANT_CODE/USERNAME/PASSWORD 后运行')

  if (providedToken) {
    await page.addInitScript((token) => sessionStorage.setItem('flowops-access-token', token), providedToken)
    await page.goto('/tickets')
  } else {
    await login(page)
  }

  await expect(page).toHaveURL(/\/tickets$/)
  await expect(page.getByRole('link', { name: /消息通知/ })).toBeVisible()

  const unreadResponse = page.waitForResponse((response) =>
    response.url().includes('/notification/unread-count') && response.status() === 200)
  await page.reload()
  await unreadResponse
  await page.getByRole('link', { name: '查看通知' }).click()
  await expect(page.getByRole('heading', { name: '消息通知' })).toBeVisible()

  const token = await page.evaluate(() => sessionStorage.getItem('flowops-access-token'))
  expect(token).toBeTruthy()
  await page.getByRole('button', { name: '退出登录' }).click()
  await expect(page).toHaveURL(/\/login$/)

  const revokedResponse = await request.get(`${apiBaseURL}/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(revokedResponse.status()).toBe(401)
  expect((await revokedResponse.json()).code).toBe(10002)
})

async function login(page: Page) {
  await page.goto('/login')
  await page.getByLabel('企业编码').fill(tenantCode!)
  await page.getByLabel('用户名').fill(username!)
  await page.getByLabel('密码').fill(password!)
  await page.getByRole('button', { name: /进入 FlowOps/ }).click()
}
