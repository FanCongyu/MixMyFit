.PHONY: test test-backend test-frontend test-e2e

test: test-backend test-frontend

test-backend:
	cd backend && mvn test

test-frontend:
	cd frontend && npm ci && npm run build && npm test -- --run

test-e2e:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File e2e/run-backend-e2e.ps1
