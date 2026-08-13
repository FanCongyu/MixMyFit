import { render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'

import App from './App.vue'

describe('App', () => {
  afterEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('renders the MixMyFit application shell', () => {
    render(App)

    expect(screen.getByText('MixMyFit')).toBeTruthy()
  })

  it('renders the authenticated placeholder route', () => {
    window.history.pushState({}, '', '/app')

    render(App)

    expect(screen.getByText('衣橱工作台')).toBeTruthy()
  })
})
