import { describe, expect, it } from 'vitest'

import { buildMenus, routes } from '@/router'

function authStoreWith(roles) {
  return {
    hasRole: roleCode => roles.includes(roleCode)
  }
}

describe('菜单与路由权限配置', () => {
  it('管理员看到全部带菜单的路由', () => {
    const menus = buildMenus(authStoreWith(['ROLE_ADMIN']))
    const titles = menus.map(menu => menu.title)

    expect(titles).toContain('用户管理')
    expect(titles).toContain('系统配置')
    expect(titles).toContain('操作日志')
    expect(titles).toContain('工单处理工作台')
  })

  it('客户只能看到与自身数据相关的菜单', () => {
    const menus = buildMenus(authStoreWith(['ROLE_CUSTOMER']))
    const titles = menus.map(menu => menu.title)

    expect(titles).toContain('工单列表')
    expect(titles).toContain('设备档案')
    expect(titles).toContain('服务评价')
    // 客户可以看到自己的客户档案（后端按数据域只返回自身记录）
    expect(titles).toContain('客户管理')
    expect(titles).not.toContain('用户管理')
    expect(titles).not.toContain('系统配置')
    expect(titles).not.toContain('售后运营仪表盘')
  })

  it('工程师看不到客户管理与系统管理菜单', () => {
    const menus = buildMenus(authStoreWith(['ROLE_ENGINEER']))
    const titles = menus.map(menu => menu.title)

    expect(titles).toContain('售后运营仪表盘')
    expect(titles).toContain('工单处理工作台')
    expect(titles).not.toContain('客户管理')
    expect(titles).not.toContain('用户管理')
  })

  it('系统管理路由声明了管理员角色，工单列表对所有登录用户开放', () => {
    const layout = routes.find(route => route.path === '/')
    const users = layout.children.find(route => route.path === 'system/users')
    const tickets = layout.children.find(route => route.path === 'tickets')

    expect(users.meta.roles).toEqual(['ROLE_ADMIN'])
    expect(tickets.meta.roles).toBeUndefined()
  })
})
