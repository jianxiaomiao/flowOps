import { randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'

const apiBase = process.env.FLOWOPS_E2E_API_BASE_URL ?? 'http://127.0.0.1:8080/flowOps'
const machine = process.env.FLOWOPS_E2E_MACHINE_CREDENTIAL
const tenant = process.env.FLOWOPS_E2E_TENANT_CODE
const username = process.env.FLOWOPS_E2E_USERNAME
const password = process.env.FLOWOPS_E2E_PASSWORD

test.use({ trace: 'off', screenshot: 'off', video: 'off' })

test('客服在工作台领取 Nexus 工单后，可在我负责的外部工单中查看', async ({ page }) => {
  if (!machine || !tenant || !username || !password) {
    test.skip(true, '需要 Nexus 接入验收脚本生成的本地凭据')
    return
  }
  const url = new URL(apiBase)
  expect(url.origin).toBe('http://127.0.0.1:8080')
  expect(url.pathname).toBe('/flowOps')
  const title = `工作台领取 ${randomUUID()}`
  const created = await fetch(`${apiBase}/integration/v1/tickets`, {
    method: 'POST',
    redirect: 'error',
    headers: { Authorization: `Bearer ${machine}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      schemaVersion: 1,
      eventId: randomUUID(),
      externalTicketId: randomUUID(),
      title,
      description: 'FlowOps 工作台验收工单',
      submittedAt: new Date().toISOString(),
    }),
  })
  expect(created.status).toBe(201)
  const { flowOpsTicketId, ticketNo } = await created.json()

  await page.goto('/login')
  await page.getByLabel('企业编码').fill(tenant)
  await page.getByLabel('用户名').fill(username)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: /进入 FlowOps/ }).click()
  await expect(page).toHaveURL(/\/tickets$/)

  await page.getByRole('button', { name: '待领取', exact: true }).click()
  const row = page.getByRole('row').filter({ hasText: title })
  await expect(row).toContainText(ticketNo)
  const response = page.waitForResponse((reply) =>
    new URL(reply.url()).pathname.endsWith(`/ticket/${flowOpsTicketId}/claim`) && reply.request().method() === 'POST')
  await row.getByRole('button', { name: '领取', exact: true }).click()
  expect((await response).status()).toBe(200)
  await expect(row).toHaveCount(0)

  await page.getByRole('button', { name: '我负责的外部工单' }).click()
  await expect(row).toContainText(ticketNo)
  await expect(row).toContainText('已接单')
})
