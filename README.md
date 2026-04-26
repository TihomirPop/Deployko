# Deployko

Deployko is the CD part of a CI/CD pipeline.

## Local Development

Start a local PostgreSQL container:

```bash
docker run --name deployko-postgres \
  -e POSTGRES_DB=deployko \
  -e POSTGRES_USER=deployko \
  -e POSTGRES_PASSWORD=deployko \
  -p 5432:5432 \
  -d postgres:18-alpine
```

Stop and remove it when it is no longer needed:

```bash
docker stop deployko-postgres
docker rm deployko-postgres
```
