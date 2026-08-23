(function () {
  const $ = (id) => document.getElementById(id);
  const log = (msg) => {
    $("log").textContent = msg;
  };
  const project = (lat, lon) => {
    const x = (lon + 7.35) * 56 + 48;
    const y = (61.2 - lat) * 70 + 28;
    return [x, y];
  };
  const svgEl = (name, attrs) => {
    const node = document.createElementNS("http://www.w3.org/2000/svg", name);
    Object.entries(attrs).forEach(([key, value]) => node.setAttribute(key, String(value)));
    return node;
  };

  let network = { nodes: [], regions: [] };
  let routeHops = [];

  function loc(id) {
    return network.nodes.find((n) => n.id === id)
      || network.regions.find((r) => r.id === id)
      || { lat: 56.5, lon: -4 };
  }

  function tickClock() {
    $("clock").textContent = new Date().toLocaleTimeString("en-GB", { hour12: false });
  }

  function draw() {
    const svg = $("map");
    while (svg.firstChild) {
      svg.removeChild(svg.firstChild);
    }
    const defs = svgEl("defs", {});
    const glow = svgEl("filter", { id: "glow" });
    glow.appendChild(svgEl("feGaussianBlur", { stdDeviation: "2.2", result: "b" }));
    const merge = svgEl("feMerge", {});
    merge.appendChild(svgEl("feMergeNode", { in: "b" }));
    merge.appendChild(svgEl("feMergeNode", { in: "SourceGraphic" }));
    glow.appendChild(merge);
    defs.appendChild(glow);
    svg.appendChild(defs);

    svg.appendChild(svgEl("path", {
      d: "M168 36 C 198 22 238 38 258 78 C 278 118 292 168 286 214 C 304 236 318 268 308 312 C 298 368 278 422 248 478 C 220 528 186 572 148 590 C 108 606 78 572 70 520 C 62 458 54 392 68 328 C 78 274 70 228 86 176 C 98 128 128 78 168 36 Z",
      fill: "#1c241c",
      stroke: "#8b8374",
      "stroke-width": "1.3"
    }));
    svg.appendChild(svgEl("path", {
      d: "M248 92 C 268 86 286 104 278 126 C 270 142 252 138 248 120 Z",
      fill: "#1c241c",
      stroke: "#8b8374",
      "stroke-width": "0.8",
      opacity: "0.7"
    }));

    network.regions.forEach((region) => {
      const [x, y] = project(region.lat, region.lon);
      svg.appendChild(svgEl("circle", { cx: x, cy: y, r: 2.2, fill: "#7d9a78", opacity: "0.75" }));
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
        "stroke-width": "2.6",
        filter: "url(#glow)",
        "stroke-linecap": "round",
        "stroke-linejoin": "round"
      }));
    }

    network.nodes
      .filter((node) => node.type !== "TRANSFER_HUB" || node.capacity > 0)
      .forEach((node) => {
        const [x, y] = project(node.lat, node.lon);
        const fill = node.type === "NATIONAL_DC" ? "#b42318" : node.type === "REGIONAL_DC" ? "#e07a2f" : "#d4b15a";
        const r = node.type === "NATIONAL_DC" ? 7.5 : node.type === "REGIONAL_DC" ? 5.8 : 4.2;
        svg.appendChild(svgEl("circle", { cx: x, cy: y, r: r + 3, fill: fill, opacity: "0.18" }));
        svg.appendChild(svgEl("circle", { cx: x, cy: y, r, fill }));
        const label = svgEl("text", {
          x: x + 9,
          y: y + 3,
          fill: "#f0e2c4",
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

  function renderHops(hops) {
    const list = $("hops");
    while (list.firstChild) {
      list.removeChild(list.firstChild);
    }
    hops.forEach((hop) => {
      const item = document.createElement("li");
      item.textContent = hop.fromId + " → " + hop.toId + "  " + hop.mode + "  £" + hop.cost.toFixed(2);
      list.appendChild(item);
    });
  }

  function renderDocks() {
    const docks = $("docks");
    while (docks.firstChild) {
      docks.removeChild(docks.firstChild);
    }
    network.nodes
      .filter((node) => node.capacity > 0)
      .forEach((node) => {
        const row = document.createElement("div");
        row.className = "dock";
        const name = document.createElement("b");
        name.textContent = node.id;
        const bar = document.createElement("div");
        bar.className = "bar";
        const fill = document.createElement("span");
        const pct = node.capacity ? Math.min(100, (node.onHand / node.capacity) * 100) : 0;
        fill.style.width = pct.toFixed(1) + "%";
        bar.appendChild(fill);
        const qty = document.createElement("span");
        qty.textContent = String(node.onHand);
        row.appendChild(name);
        row.appendChild(bar);
        row.appendChild(qty);
        docks.appendChild(row);
      });
  }

  async function boot() {
    const res = await fetch("/api/v1/network");
    network = await res.json();
    $("netStamp").textContent = network.fulfillmentNodes + " nodes · " + network.demandRegions + " regions";
    fillRegions();
    draw();
    renderDocks();
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
    renderHops(routeHops);
    $("routeMeta").textContent = data.slaMet
      ? data.sourceNodeId + " → " + data.regionId + " · " + data.algorithm
        + " · £" + Number(data.cost).toFixed(2) + " · " + data.etaHours + "h"
      : "No SLA-feasible path from stocked nodes.";
    log(JSON.stringify(data, null, 2));
  };

  $("placeBtn").onclick = async () => {
    const data = await (await fetch("/api/v1/placement", { method: "POST" })).json();
    const lines = Object.entries(data.unitsAtNode).map(([key, value]) => key.padEnd(8) + " " + value);
    log("Min-cost flow placed " + data.totalFlow + " units · cost " + Number(data.totalCost).toFixed(1)
      + " · unmet " + data.unmetDemand + "\n" + lines.join("\n"));
  };

  $("simBtn").onclick = async () => {
    log("Running deterministic 90-day duel…");
    const data = await (await fetch("/api/v1/simulate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ days: 90, seed: 42 })
    })).json();
    $("kpiFill").textContent = (data.flowforge.fillRate * 100).toFixed(1) + "%";
    $("kpiCost").textContent = "-" + (data.costReduction * 100).toFixed(1) + "%";
    const ok = data.flowforge.invariants && data.flowforge.invariants.ok;
    log("FlowForge fill " + (data.flowforge.fillRate * 100).toFixed(2) + "%  cost " + data.flowforge.totalCost.toFixed(0)
      + "\nGreedy    fill " + (data.greedy.fillRate * 100).toFixed(2) + "%  cost " + data.greedy.totalCost.toFixed(0)
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

  tickClock();
  setInterval(tickClock, 1000);
  boot().catch((err) => log("Board failed: " + err.message));
})();
