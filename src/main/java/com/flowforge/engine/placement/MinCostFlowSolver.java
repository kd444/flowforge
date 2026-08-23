package com.flowforge.engine.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Successive shortest path min-cost flow with reduced-cost potentials.
 * Costs are integer (scaled) so residual updates stay exact.
 */
public final class MinCostFlowSolver {

    public record Result(int flow, long cost, List<int[]> used) {
    }

    private static final class Edge {
        final int to;
        int cap;
        final int originalCap;
        final long cost;
        final int rev;
        final boolean original;

        Edge(int to, int cap, long cost, int rev, boolean original) {
            this.to = to;
            this.cap = cap;
            this.originalCap = cap;
            this.cost = cost;
            this.rev = rev;
            this.original = original;
        }
    }

    private final List<List<Edge>> graph;
    private final List<int[]> originals = new ArrayList<>();

    public MinCostFlowSolver(int nodeCount) {
        graph = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            graph.add(new ArrayList<>());
        }
    }

    public void addEdge(int from, int to, int cap, long cost) {
        Edge forward = new Edge(to, cap, cost, graph.get(to).size(), true);
        Edge backward = new Edge(from, 0, -cost, graph.get(from).size(), false);
        graph.get(from).add(forward);
        graph.get(to).add(backward);
        originals.add(new int[]{from, to, cap, 0});
    }

    public Result minCostFlow(int source, int sink, int maxFlow) {
        int n = graph.size();
        long[] pot = new long[n];
        int flow = 0;
        long cost = 0;
        while (flow < maxFlow) {
            long[] dist = new long[n];
            Arrays.fill(dist, Long.MAX_VALUE / 4);
            int[] prevNode = new int[n];
            int[] prevEdge = new int[n];
            Arrays.fill(prevNode, -1);
            dist[source] = 0;
            PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
            pq.add(new long[]{0, source});
            while (!pq.isEmpty()) {
                long[] cur = pq.poll();
                int u = (int) cur[1];
                if (cur[0] != dist[u]) {
                    continue;
                }
                List<Edge> edges = graph.get(u);
                for (int i = 0; i < edges.size(); i++) {
                    Edge e = edges.get(i);
                    if (e.cap <= 0) {
                        continue;
                    }
                    long nd = dist[u] + e.cost + pot[u] - pot[e.to];
                    if (nd < dist[e.to]) {
                        dist[e.to] = nd;
                        prevNode[e.to] = u;
                        prevEdge[e.to] = i;
                        pq.add(new long[]{nd, e.to});
                    }
                }
            }
            if (dist[sink] >= Long.MAX_VALUE / 8) {
                break;
            }
            for (int i = 0; i < n; i++) {
                if (dist[i] < Long.MAX_VALUE / 8) {
                    pot[i] += dist[i];
                }
            }
            int add = maxFlow - flow;
            for (int v = sink; v != source; v = prevNode[v]) {
                Edge e = graph.get(prevNode[v]).get(prevEdge[v]);
                add = Math.min(add, e.cap);
            }
            for (int v = sink; v != source; v = prevNode[v]) {
                Edge e = graph.get(prevNode[v]).get(prevEdge[v]);
                e.cap -= add;
                graph.get(v).get(e.rev).cap += add;
            }
            flow += add;
            cost += (long) add * pot[sink];
        }
        List<int[]> used = new ArrayList<>();
        for (int from = 0; from < n; from++) {
            for (Edge edge : graph.get(from)) {
                if (!edge.original) {
                    continue;
                }
                int sent = edge.originalCap - edge.cap;
                if (sent > 0) {
                    used.add(new int[]{from, edge.to, sent});
                }
            }
        }
        return new Result(flow, cost, used);
    }
}
