package at.yawk.profiler.graph;

import at.yawk.profiler.sampler.StackGraph;
import com.google.common.io.ByteStreams;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.wintersleep.graphviz.DiGraph;
import org.wintersleep.graphviz.Edge;
import org.wintersleep.graphviz.Node;
import org.wintersleep.graphviz.NodeAttributeList;

/**
 * @author yawkat
 */
@Slf4j
public class GraphvizRenderer implements GraphRenderer {
    @Override
    public String renderSvg(StackGraph graph) {
        String dot = makeDot(graph);
        String svg;
        try {
            svg = renderSvg(dot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return svg;
    }

    private String renderSvg(String dot) throws IOException {
        log.debug("Rendering DOT file as SVG ({} characters)", dot.length());
        Process process = new ProcessBuilder("dot", "-Tsvg").start();
        process.getOutputStream().write(dot.getBytes(StandardCharsets.UTF_8));
        process.getOutputStream().close();
        byte[] svgBytes = ByteStreams.toByteArray(process.getInputStream());
        return new String(svgBytes, StandardCharsets.UTF_8);
    }

    private String makeDot(StackGraph graph) {
        log.debug("Rendering graph to DOT");

        DiGraph diGraph = new DiGraph("G");
        Map<StackGraph.Node, Node> registry = new HashMap<>();
        StackGraph.Node root = graph.getRoot();
        for (StackGraph.Child child : root.children()) {
            StackGraph.Node target = child.getTarget();
            add(diGraph, target, root.getTotalTime() / 100, target.getTotalTime(), registry);
        }
        StringWriter sw = new StringWriter();
        diGraph.print(new PrintWriter(sw));
        return sw.toString();
    }

    private void add(DiGraph graph, StackGraph.Node node, int min, int max,
                     Map<StackGraph.Node, Node> registry) {
        if (registry.containsKey(node)) { return; }
        if (node.getTotalTime() < min) {
            registry.put(node, null);
            return;
        }
        Node n = graph.addNode("n" + registry.size());
        String clazz = node.getClassName().substring(node.getClassName().lastIndexOf('.') + 1);
        NodeAttributeList attr = n.addAttributeList();
        attr.setLabel(clazz + "\n" + node.getMethodName() + "\n" + node.getSelfTime() + "/" + node.getTotalTime());
        attr.setShape("box");

        float magnitude = Math.min(3F / 2, (float) node.getSelfTime() / max);
        float hue = 1F / 3 - magnitude * 1F / 3; // green to purple
        attr.setColor(new Color(Color.HSBtoRGB(hue, 1, 1)));

        registry.put(node, n);
        for (StackGraph.Child child : node.children()) {
            add(graph, child.getTarget(), min, max, registry);
            Node next = registry.get(child.getTarget());
            if (next != null) {
                Edge edge = graph.addEdge(n, next);
                edge.addAttributeList().setLabel(String.valueOf(child.getEnterCount()));
            }
        }
    }
}
