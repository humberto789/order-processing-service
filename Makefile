APP_NAME=order-processing-service

.PHONY: setup up down build test clean logs db-migrate restart

setup:
	docker compose build

up:
	docker compose up -d

down:
	docker compose down

build:
	mvn -q clean package -DskipTests

test:
	mvn test

clean:
	mvn clean
	docker compose down -v

logs:
	docker compose logs -f app

db-migrate:
	@echo "Database migrations are handled automatically by JPA/Hibernate (ddl-auto=update)"
	@echo "To force schema recreation, run: make clean && make up"

restart:
	docker compose restart app

status:
	docker compose ps

health:
	@curl -s http://localhost:8080/actuator/health | python3 -m json.tool || echo "Service not available"
