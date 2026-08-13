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

export function resolveRoute(pathname: string = window.location.pathname): AppRoute {
  return pathname === authenticatedPlaceholderRoute.path
    ? authenticatedPlaceholderRoute
    : defaultRoute
}
