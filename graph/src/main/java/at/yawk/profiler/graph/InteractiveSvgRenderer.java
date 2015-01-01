package at.yawk.profiler.graph;

import at.yawk.profiler.sampler.StackGraph;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
public class InteractiveSvgRenderer implements GraphRenderer {
    private static final String TRANSFORM_XSLT;
    private static final String DEFAULT_SVGPAN_URI = "https://www.cyberz.org/projects/SVGPan/SVGPan.js";

    private final GraphRenderer renderer;
    private final String svgPan;

    static {
        try (InputStream is = InteractiveSvgRenderer.class.getResourceAsStream("svgpan.xslt")) {
            TRANSFORM_XSLT = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InteractiveSvgRenderer(GraphRenderer renderer) {
        this(renderer, DEFAULT_SVGPAN_URI);
    }

    @Override
    public String renderSvg(StackGraph graph) {
        String svg = renderer.renderSvg(graph);

        log.debug("Adding SVGPan to SVG output ({} characters)", svg.length());

        try {
            StreamSource xslt = new StreamSource(new StringReader(TRANSFORM_XSLT));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(xslt);
            transformer.setParameter("svgpan-uri", svgPan);

            StringWriter sw = new StringWriter();
            transformer.transform(
                    new StreamSource(new StringReader(svg)),
                    new StreamResult(sw)
            );

            return sw.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
