(function () {
  const $ = (id) => document.getElementById(id);
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
  const clear = (el) => {
    while (el.firstChild) {
      el.removeChild(el.firstChild);
    }
  };
  const text = (el, value) => {
    el.textContent = value;
  };
  const addLine = (parent, value, className) => {
    const p = document.createElement("p");
    if (className) {
      p.className = className;
    }
    p.textContent = value;
    parent.appendChild(p);
    return p;
  };

  let network = { nodes: [], regions: [], data: {} };
  let routeHops = [];
  let selectedRegionId = "R01";

  function loc(id) {
    return network.nodes.find((n) => n.id === id)
      || network.regions.find((r) => r.id === id)
      || { lat: 56.5, lon: -4 };
  }

  function selectedRegion() {
    return network.regions.find((r) => r.id === selectedRegionId) || network.regions[0];
  }

  function nodeById(id) {
    return network.nodes.find((n) => n.id === id);
  }

  function tickClock() {
    $("clock").textContent = new Date().toLocaleTimeString("en-GB", { hour12: false });
  }

  function showResult(title, bodyFn) {
    const box = $("result");
    clear(box);
    const heading = document.createElement("p");
    heading.className = "hint";
    heading.textContent = title;
    box.appendChild(heading);
    bodyFn(box);
  }

  function table(box, headers, rows) {
    const tbl = document.createElement("table");
    const head = document.createElement("tr");
    headers.forEach((h) => {
      const th = document.createElement("th");
      th.textContent = h;
      head.appendChild(th);
    });
    tbl.appendChild(head);
    rows.forEach((row) => {
      const tr = document.createElement("tr");
      row.forEach((cell) => {
        const td = document.createElement("td");
        td.textContent = cell;
        tr.appendChild(td);
      });
      tbl.appendChild(tr);
    });
    box.appendChild(tbl);
  }

  function draw() {
    const svg = $("map");
    clear(svg);
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

    const region = selectedRegion();
    network.regions.forEach((item) => {
      const [x, y] = project(item.lat, item.lon);
      const active = item.id === selectedRegionId;
      svg.appendChild(svgEl("circle", {
        cx: x,
        cy: y,
        r: active ? 5 : 2.2,
        fill: active ? "#f0e2c4" : "#7d9a78",
        opacity: active ? "1" : "0.7"
      }));
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
        const assigned = region && region.assignedNodeId === node.id;
        const fill = node.type === "NATIONAL_DC" ? "#b42318" : node.type === "REGIONAL_DC" ? "#e07a2f" : "#d4b15a";
        const r = node.type === "NATIONAL_DC" ? 7.5 : node.type === "REGIONAL_DC" ? 5.8 : 4.2;
        svg.appendChild(svgEl("circle", { cx: x, cy: y, r: r + (assigned ? 6 : 3), fill, opacity: assigned ? "0.35" : "0.18" }));
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
    clear(select);
    network.regions.forEach((region) => {
      const option = document.createElement("option");
      option.value = region.id;
      option.textContent = region.id + " · " + region.name;
      select.appendChild(option);
    });
    select.value = selectedRegionId;
  }

  function renderBrief() {
    const region = selectedRegion();
    if (!region) {
      return;
    }
    const node = nodeById(region.assignedNodeId);
    text($("briefNode"), region.assignedNodeId + (node ? " · " + node.name : ""));
    text($("briefForecast"), Number(region.forecast || 0).toFixed(1) + " units");
    text($("briefStock"), node ? node.onHand + " / " + node.capacity : "—");
  }

  function renderHops(hops) {
    const list = $("hops");
    clear(list);
    hops.forEach((hop) => {
      const item = document.createElement("li");
      item.textContent = hop.fromId + " → " + hop.toId + "  " + hop.mode + "  £" + hop.cost.toFixed(2);
      list.appendChild(item);
    });
  }

  function renderDocks() {
    const docks = $("docks");
    clear(docks);
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
        fill.style.width = Math.min(100, (node.onHand / node.capacity) * 100).toFixed(1) + "%";
        bar.appendChild(fill);
        const qty = document.createElement("span");
        qty.textContent = String(node.onHand);
        row.appendChild(name);
        row.appendChild(bar);
        row.appendChild(qty);
        docks.appendChild(row);
      });
  }

  function renderProvenance() {
    const data = network.data || {};
    text($("provSource"), data.source || "M5 retail schema");
    text($("provHistory"), (data.historyDays || "—") + " days · " + (data.skuCount || "—") + " SKUs");
    text($("provStock"), (data.onHandUnits || 0).toLocaleString() + " / " + (data.storageCapacity || 0).toLocaleString());
    text($("provForecast"), (data.dailyForecastUnits || 0) + " units · " + (data.forecastMethod || ""));
    text($("kpiStock"), String(data.onHandUnits || 0));
    text($("kpiForecast"), String(data.dailyForecastUnits || 0));
    $("netStamp").textContent = network.fulfillmentNodes + " nodes · " + network.demandRegions + " regions";
  }

  function placeInventory() {
    return fetch("/api/v1/placement", { method: "POST" })
      .then((res) => res.json())
      .then((data) => {
        showResult(
          "Min-cost flow placed " + data.totalFlow + " units using on-hand stock and the M5 daily forecast. Unmet " + data.unmetDemand + ".",
          (box) => {
            table(box, ["Node", "Units"], Object.entries(data.unitsAtNode));
          }
        );
      });
  }

  async function boot() {
    const res = await fetch("/api/v1/network");
    network = await res.json();
    selectedRegionId = network.regions[0] ? network.regions[0].id : "R01";
    fillRegions();
    renderProvenance();
    renderBrief();
    draw();
    renderDocks();
    showResult("Live state loaded from the engine — not a chat wrapper.", (box) => {
      addLine(box, "Demand: " + (network.data && network.data.source) + ", " + (network.data && network.data.historyDays) + " days, " + (network.data && network.data.skuCount) + " store-item series.", "hint");
      addLine(box, "Placement and routing both read the on-hand column under the map and the region forecast in Dispatch.", "hint");
    });
  }

  $("region").onchange = () => {
    selectedRegionId = $("region").value;
    routeHops = [];
    renderBrief();
    draw();
  };

  $("routeBtn").onclick = async () => {
    selectedRegionId = $("region").value;
    const res = await fetch("/api/v1/route", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        regionId: selectedRegionId,
        departHour: Number($("hour").value),
        slaHours: Number($("sla").value),
        algorithm: $("algo").value,
        requireStock: true
      })
    });
    const data = await res.json();
    routeHops = data.hops || [];
    draw();
    renderHops(routeHops);
    $("routeMeta").textContent = data.slaMet
      ? data.sourceNodeId + " → " + data.regionId + " · " + data.algorithm + " · £" + Number(data.cost).toFixed(2) + " · " + data.etaHours + "h"
      : "No SLA-feasible path from stocked nodes.";
    showResult("Route uses current on-hand stock and time-of-day lane costs.", (box) => {
      table(box, ["From", "To", "Mode", "£"], (data.hops || []).map((hop) => [
        hop.fromId, hop.toId, hop.mode, hop.cost.toFixed(2)
      ]));
    });
  };

  $("placeBtn").onclick = placeInventory;
  $("placeBtn2").onclick = placeInventory;

  $("simBtn").onclick = async () => {
    showResult("Running 90-day comparison on the same stock and forecast…", () => {});
    const data = await (await fetch("/api/v1/simulate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ days: 90, seed: 42 })
    })).json();
    $("kpiFill").classList.remove("pending");
    $("kpiCost").classList.remove("pending");
    text($("kpiFill"), (data.flowforge.fillRate * 100).toFixed(1) + "%");
    text($("kpiCost"), "-" + (data.costReduction * 100).toFixed(1) + "%");
    showResult("FlowForge vs nearest-node last-mile greedy. Invariants " + (data.flowforge.invariants.ok ? "hold" : "failed") + ".", (box) => {
      table(box, ["Policy", "Fill", "Cost"], [
        ["FlowForge", (data.flowforge.fillRate * 100).toFixed(2) + "%", Math.round(data.flowforge.totalCost).toLocaleString()],
        ["Greedy", (data.greedy.fillRate * 100).toFixed(2) + "%", Math.round(data.greedy.totalCost).toLocaleString()]
      ]);
    });
  };

  $("agentBtn").onclick = async () => {
    const data = await (await fetch("/api/v1/agent/tick?applyIfAccepted=true", { method: "POST" })).json();
    showResult(data.status + " — " + data.reason, (box) => {
      addLine(box, (data.proposal && data.proposal.rationale) || "Heuristic planner proposed stock and lane edits.", "hint");
      const deltas = data.proposal && data.proposal.safetyStockDeltas ? Object.entries(data.proposal.safetyStockDeltas) : [];
      if (deltas.length) {
        table(box, ["Node", "Safety-stock delta"], deltas);
      }
    });
  };

  tickClock();
  setInterval(tickClock, 1000);
  boot().catch((err) => {
    showResult("Board failed to load: " + err.message, () => {});
  });
})();
