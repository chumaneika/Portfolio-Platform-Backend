# Production HTTPS

1. Point the domain's DNS A/AAAA records to the server. Open inbound TCP ports 80 and 443.
2. Copy `.env.production.example` to `.env.production`. Set `SITE_DOMAIN` to the domain name (without `https://`) and fill in all database passwords and `JWT_SECRET`.
3. Start the stack with:

   ```sh
   docker compose --env-file .env.production -f docker.compose.yaml -f docker.compose.prod.yaml up -d --build
   ```

Caddy accepts public traffic and automatically obtains and renews the TLS certificate. Its `/data` volume must be preserved across deployments. The production Compose configuration publishes only ports 80 and 443; the API Gateway and databases remain on private Docker networks.

The gateway trusts `X-Client-IP` only in the production profile. Caddy overwrites this header with the address of its direct peer, so rate limits apply per visitor when clients connect directly to Caddy. If a CDN or another proxy is added in front of Caddy, its trusted proxy settings and the IP forwarding path must be reviewed before enabling it.

After starting the stack, run `docker compose --env-file .env.production -f docker.compose.yaml -f docker.compose.prod.yaml ps`. Java services, PostgreSQL, and Redis should report `healthy`. The Java checks call `/actuator/health/readiness` inside each container; database services also check their database connection, and the gateway checks Redis. If a service is `unhealthy`, inspect its logs with `docker compose --env-file .env.production -f docker.compose.yaml -f docker.compose.prod.yaml logs <service-name>`.
