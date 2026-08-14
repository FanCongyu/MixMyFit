.PHONY: test test-backend test-frontend test-e2e

LOCAL_MVN := $(firstword $(wildcard $(subst \,/,$(USERPROFILE))/.m2/wrapper/dists/apache-maven-*-bin/*/apache-maven-*/bin/mvn.cmd))
MVN ?= $(if $(LOCAL_MVN),$(LOCAL_MVN),mvn)

test: test-backend test-frontend

test-backend:
	cd backend && $(MVN) test

test-frontend:
	cd frontend && npm ci && npm run build && npm test -- --run

test-e2e:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -File e2e/run-backend-e2e.ps1
