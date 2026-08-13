export type AppRoute = {
  path: string
  title: string
}

const defaultRoute: AppRoute = {
  path: '/',
  title: 'MixMyFit'
}

const authenticatedPlaceholderRoute: AppRoute = {
  path: '/app',
  title: '衣橱工作台'
}

const routes: AppRoute[] = [
  defaultRoute,
  authenticatedPlaceholderRoute,
  {
    path: '/clothes',
    title: '衣物库'
  },
  {
    path: '/login',
    title: 'Log in'
  },
  {
    path: '/register',
    title: 'Create account'
  },
  {
    path: '/profile',
    title: 'Profile'
  }
]

export function resolveRoute(pathname: string = window.location.pathname): AppRoute {
  return routes.find((route) => route.path === pathname) || defaultRoute
}
