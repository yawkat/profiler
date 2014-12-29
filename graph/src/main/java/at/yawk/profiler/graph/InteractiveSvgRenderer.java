package at.yawk.profiler.graph;

import at.yawk.profiler.sampler.StackGraph;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.dom.util.SAXDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
public class InteractiveSvgRenderer implements GraphRenderer {
    private final GraphRenderer renderer;
    private final String svgPan;

    public InteractiveSvgRenderer(GraphRenderer renderer) {
        this(renderer, "https://www.cyberz.org/projects/SVGPan/SVGPan.js");
    }

    @Override
    public String renderSvg(StackGraph graph) {
        String svg = renderer.renderSvg(graph);

        log.debug("Adding SVGPan to SVG output ({} characters)", svg.length());

        Document doc;
        try {
            String parserName = XMLResourceDescriptor.getXMLParserClassName();
            SAXDocumentFactory factory = new SAXDocumentFactory(
                    DOMImplementationRegistry.newInstance().getDOMImplementation("XML 1.0"),
                    parserName
            );
            doc = factory.createDocument(null, new StringReader(svg));
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        Element root = doc.getDocumentElement();
        root.removeAttribute("viewBox");

        Element oldViewport = null;

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node node = root.getChildNodes().item(i);
            if (node.getNodeName().equalsIgnoreCase("g")) {
                oldViewport = (Element) node;
                break;
            }
        }

        if (oldViewport == null) {
            throw new UnsupportedOperationException("Missing g element");
        }

        Element newViewport = doc.createElement("g");
        newViewport.setAttribute("id", "viewport");

        root.replaceChild(newViewport, oldViewport);
        newViewport.appendChild(oldViewport);

        Element script = doc.createElement("script");
        script.setAttribute("xlink:href", svgPan);
        root.insertBefore(script, root.getFirstChild());

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            transformer.transform(new DOMSource(doc), result);

            return sw.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
