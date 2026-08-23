# FlowForge

Multi-echelon inventory placement and fulfillment routing engine for a 12-node Scottish fulfillment network serving 60 demand regions.

Inventory placement is solved as a min-cost flow over a demand-forecast-weighted graph. Fulfillment routing uses constrained shortest paths (A* and bidirectional Dijkstra) with time-dependent edge costs, then binds a source node and multi-modal lane to a delivery-promise SLA.

An agentic control loop proposes safety-stock, lane-capacity, and node-to-region changes. A deterministic simulator with a flow-conservation invariant checker accepts, rejects, or rolls those changes back.

## Stack

- Java 17 / Spring Boot 3.4 API
- Redis-backed path-label precomputation (in-memory fallback)
- Docker + AWS ECS / Fargate + ElastiCache
- Demand series in public M5 retail schema (synthetic generator included; swap in the real M5 file to retrain)

## Quick start

```bash
./mvnw test
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) for the Forth Grid control board, or [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) for the API.

Docker:

```bash
docker compose up --build
```

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/network` | 12 nodes, 60 regions, lane counts |
| POST | `/api/v1/route` | SLA-constrained source + multi-modal path |
| POST | `/api/v1/placement` | Min-cost inventory placement |
| POST | `/api/v1/simulate` | FlowForge vs greedy, with invariant report |
| POST | `/api/v1/agent/tick` | Propose → simulate → accept / reject / rollback |
| GET | `/api/v1/forecast/{regionId}` | Seasonal-naive / Croston horizon |
| POST | `/api/v1/bench/routing` | Local routing latency percentiles |

Example route:

```bash
curl -s localhost:8080/api/v1/route \
  -H 'Content-Type: application/json' \
  -d '{"regionId":"R40","departHour":9,"slaHours":24,"algorithm":"ASTAR"}'
```

## Agentic loop

Set `LLM_PROVIDER=openai` and `OPENAI_API_KEY` to let an LLM propose network edits. Without a key the heuristic planner still runs. Every proposal is applied to a copied network, simulated, and checked for flow conservation before it can land on the live graph.

## Deployment

- `Dockerfile` — multi-stage Temurin 17 image
- `docker-compose.yml` — API + Redis
- `infra/ecs/cloudformation.yaml` — ECS Fargate, ALB, ElastiCache
- `infra/loadtest/k6-routing.js` — 1,200 RPS arrival-rate script (`k6 run -e BASE_URL=... infra/loadtest/k6-routing.js`)

## Tests

```bash
./mvnw test
```

Covers successive-shortest-path min-cost flow, A* / bidirectional search, Croston + seasonal-naive forecasts, simulator conservation, and agent reject/accept paths.

On the bundled Scottish network a 90-day duel (`POST /api/v1/simulate`) holds 100% fill and cuts transport+holding cost about 13% versus nearest-node last-mile greedy. `POST /api/v1/bench/routing` measures sub-millisecond decisions on this graph; use the k6 script against ECS for the 1,200 RPS / 85ms p99 target.
