import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    routing: {
      executor: "constant-arrival-rate",
      rate: 1200,
      timeUnit: "1s",
      duration: "2m",
      preAllocatedVUs: 200,
      maxVUs: 600,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(99)<85"],
  },
};

const regions = [
  "R01", "R04", "R07", "R09", "R11", "R15", "R25", "R33", "R39", "R40", "R50",
];

export default function () {
  const regionId = regions[Math.floor(Math.random() * regions.length)];
  const res = http.post(
    `${__ENV.BASE_URL || "http://localhost:8080"}/api/v1/route`,
    JSON.stringify({
      regionId,
      departHour: Math.floor(Math.random() * 24),
      slaHours: 24,
      algorithm: "AUTO",
      requireStock: false,
    }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "status 200": (r) => r.status === 200 });
  sleep(0.001);
}
