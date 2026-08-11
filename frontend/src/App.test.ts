import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import App from './App.vue'

describe('App', () => {
  it('renders the MixMyFit application shell', () => {
    render(App)

    expect(screen.getByText('MixMyFit')).toBeTruthy()
  })
})
