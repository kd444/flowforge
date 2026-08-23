(function () {
  const $ = (id) => document.getElementById(id);
  const log = (msg) => {
    $("log").textContent = msg;
  };
  const project = (lat, lon) => {
    const x = (lon + 7.2) * 58 + 36;
    const y = (61.1 - lat) * 72 + 18;
    return [x, y];
  };
  const svgEl = (name, attrs) => {
    const node = document.createElementNS("http://www.w3.org/2000/svg", name);
    Object.entries(attrs).forEach(([key, value]) => node.setAttribute(key, value));
    return node;
  };

  let network = { nodes: [], regions: [] };
  let routeHops = [];

  function loc(id) {
    return network.nodes.find((n) => n.id === id)
      || network.regions.find((r) => r.id === id)
      || { lat: 56.5, lon: -4 };
  }

  function draw() {
    const svg = $("map");
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
    }
    svg.appendChild(svgEl("path", {
      d: "M140 40 C 180 30, 230 70, 250 120 C 270 180, 300 210, 290 280 C 280 360, 250 430, 210 500 C 170 560, 130 590, 90 560 C 50 520, 40 430, 60 340 C 70 250, 80 160, 110 80 Z",
      fill: "#1b221c",
      stroke: "#8a8476",
      "stroke-width": "1.2"
    }));
    network.regions.forEach((region) => {
      const [x, y] = project(region.lat, region.lon);
      svg.appendChild(svgEl("circle", { cx: x, cy: y, r: 2.1, fill: "#6f8a73", opacity: "0.7" }));
    });
    if (routeHops.length) {
      const pairs = [];
      routeHops.forEach((hop, index) => {
        pairs.push(project(loc(hop.fromId).lat, loc(hop.fromId).lon).join(","));
        if (index === routeHops.length - 1) {
          pairs.push(project(loc(hop.toId).lat, loc(hop.toId).lon).join(","));
        }
      });
      svg.appendChild(svgEl("polyline", {
        points: pairs.join(" "),
        fill: "none",
        stroke: "#b42318",
        "stroke-width": "2.2"
      }));
    }
    network.nodes
      .filter((node) => node.capacity > 0 || node.type !== "TRANSFER_HUB")
      .forEach((node) => {
        const [x, y] = project(node.lat, node.lon);
        const fill = node.type === "NATIONAL_DC" ? "#b42318" : node.type === "REGIONAL_DC" ? "#e07a2f" : "#c9a227";
        const r = node.type === "NATIONAL_DC" ? 7 : node.type === "REGIONAL_DC" ? 5.5 : 4;
        svg.appendChild(svgEl("circle", { cx: x, cy: y, r, fill }));
        const label = svgEl("text", {
          x: x + 8,
          y: y + 3,
          fill: "#e7d6b8",
          "font-size": "9",
          "font-family": "IBM Plex Mono"
        });
        label.textContent = node.id;
        svg.appendChild(label);
      });
  }

  function fillRegions() {
    const select = $("region");
    while (select.firstChild) {
      select.removeChild(select.firstChild);
    }
    network.regions.forEach((region) => {
      const option = document.createElement("option");
      option.value = region.id;
      option.textContent = region.id + " · " + region.name;
      select.appendChild(option);
    });
  }

  async function boot() {
    const res = await fetch("/api/v1/network");
    network = await res.json();
    fillRegions();
    draw();
    log("Board live. " + network.fulfillmentNodes + " fulfillment nodes, "
      + network.demandRegions + " demand regions, " + network.laneCount + " lanes.");
  }

  $("routeBtn").onclick = async () => {
    const payload = {
      regionId: $("region").value,
      departHour: Number($("hour").value),
      slaHours: Number($("sla").value),
      algorithm: $("algo").value,
      requireStock: true
    };
    const res = await fetch("/api/v1/route", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    routeHops = data.hops || [];
    draw();
    $("routeMeta").textContent = data.slaMet
      ? data.sourceNodeId + " → " + data.regionId + " via " + data.algorithm
        + " · £" + data.cost.toFixed(2) + " · " + data.etaHours + "h · " + data.computeMicros + "µs"
      : "No SLA-feasible path from stocked nodes.";
    log(JSON.stringify(data, null, 2));
  };

  $("placeBtn").onclick = async () => {
    const data = await (await fetch("/api/v1/placement", { method: "POST" })).json();
    const lines = Object.entries(data.unitsAtNode).map(([key, value]) => key.padEnd(8) + " " + value);
    log("Min-cost flow placed " + data.totalFlow + " units · cost " + data.totalCost.toFixed(1)
      + " · unmet " + data.unmetDemand + "\n" + lines.join("\n"));
  };

  $("simBtn").onclick = async () => {
    log("Running deterministic 90-day duel...");
    const data = await (await fetch("/api/v1/simulate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ days: 90, seed: 42 })
    })).json();
    $("kpiFill").textContent = (data.flowforge.fillRate * 100).toFixed(1) + "%";
    $("kpiCost").textContent = "-" + (data.costReduction * 100).toFixed(1) + "%";
    const ok = data.flowforge.invariants && data.flowforge.invariants.ok;
    log("FlowForge fill " + (data.flowforge.fillRate * 100).toFixed(2) + "% cost " + data.flowforge.totalCost.toFixed(0)
      + "\nGreedy    fill " + (data.greedy.fillRate * 100).toFixed(2) + "% cost " + data.greedy.totalCost.toFixed(0)
      + "\nCost reduction " + (data.costReduction * 100).toFixed(2) + "%"
      + "\nInvariants " + (ok ? "HOLD" : "BROKEN"));
  };

  $("agentBtn").onclick = async () => {
    const data = await (await fetch("/api/v1/agent/tick?applyIfAccepted=true", { method: "POST" })).json();
    $("kpiAgent").textContent = data.status;
    log(data.status + ": " + data.reason + "\n" + JSON.stringify(data.proposal, null, 2));
  };

  $("benchBtn").onclick = async () => {
    const data = await (await fetch("/api/v1/bench/routing?iterations=300", { method: "POST" })).json();
    $("kpiP99").textContent = data.p99Ms + "ms";
    log("Routing bench n=" + data.iterations + "\np50 " + data.p50Ms + "ms · p95 " + data.p95Ms + "ms · p99 " + data.p99Ms + "ms");
  };

  boot().catch((err) => log("Board failed: " + err.message));
})();
