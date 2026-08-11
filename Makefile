.PHONY: test test-backend test-frontend

test: test-backend test-frontend

test-backend:
	cd backend && mvn test

test-frontend:
	cd frontend && npm ci && npm run build && npm test -- --run
