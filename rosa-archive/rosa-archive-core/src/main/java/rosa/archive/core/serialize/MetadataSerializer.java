package rosa.archive.core.serialize;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataSerializer implements Serializer<Map<String, BookMetadata>> {
    @Override
    public Map<String, BookMetadata> read(InputStream is, List<String> errors) throws IOException {
        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildMetadata(doc);

        } catch (ParserConfigurationException e) {
            String reason = "Failed to build Document.";
            errors.add(reason);
            throw new IOException(reason, e);
        } catch (SAXException e) {
            String reason = "Failed to parse input stream.";
            errors.add(reason);
            throw new IOException(reason, e);
        }
    }

    @Override
    public void write(Map<String, BookMetadata> metadataMap, OutputStream out) throws IOException {
        Document doc = newDocument();

        Element root = doc.createElement("book");
        doc.appendChild(root);

        boolean first = true;
        for (String lang : metadataMap.keySet()) {
            BookMetadata metadata = metadataMap.get(lang);

            if (first) {
                first = false;

                root.setAttribute("illustrations", String.valueOf(metadata.getNumberOfIllustrations()));
                root.setAttribute("pages", String.valueOf(metadata.getNumberOfPages()));
                // TODO title

                Element dimensions = doc.createElement("dimensions");
                root.appendChild(dimensions);
                dimensions.setAttribute("unit", "mm");
                dimensions.setAttribute("width", String.valueOf(metadata.getWidth()));
                dimensions.setAttribute("height", String.valueOf(metadata.getHeight()));

                Element texts = doc.createElement("texts");
                root.appendChild(texts);

                for (BookText text : metadata.getTexts()) {
                    Element el = doc.createElement("text");
                    texts.appendChild(el);

                    el.setAttribute("id", text.getId());
                    el.setAttribute("title", text.getTitle());
                    el.setAttribute("start", text.getFirstPage());
                    el.setAttribute("end", text.getLastPage());
                    el.setAttribute("pages", String.valueOf(text.getNumberOfPages()));
                    el.setAttribute("illustrations", String.valueOf(text.getNumberOfIllustrations()));
                    el.setAttribute("linesPerColumn", String.valueOf(text.getLinesPerColumn()));
                    el.setAttribute("leavesPerGathering", String.valueOf(text.getLeavesPerGathering()));
                    el.setAttribute("columnsPerPage", String.valueOf(text.getColumnsPerPage()));
                }
            }

            Element bibliography = doc.createElement("bibliography");
            root.appendChild(bibliography);
            bibliography.setAttribute("lang", lang);

            Element date = doc.createElement("date");
            bibliography.appendChild(date);
            date.setAttribute("start", String.valueOf(metadata.getYearStart()));
            date.setAttribute("end", String.valueOf(metadata.getYearEnd()));
            date.appendChild(doc.createTextNode(metadata.getDate()));

            Element type = doc.createElement("type");
            bibliography.appendChild(type);
            type.appendChild(doc.createTextNode(metadata.getType()));

            Element commonName = doc.createElement("commonName");
            bibliography.appendChild(commonName);
            commonName.appendChild(doc.createTextNode(metadata.getCommonName()));

            Element material = doc.createElement("material");
            bibliography.appendChild(material);
            material.appendChild(doc.createTextNode(metadata.getMaterial()));

            Element origin = doc.createElement("origin");
            bibliography.appendChild(origin);
            origin.appendChild(doc.createTextNode(metadata.getOrigin()));

            Element currentLocation = doc.createElement("currentLocation");
            bibliography.appendChild(currentLocation);
            currentLocation.appendChild(doc.createTextNode(metadata.getCurrentLocation()));

            Element repository = doc.createElement("repository");
            bibliography.appendChild(repository);
            repository.appendChild(doc.createTextNode(metadata.getRepository()));

            Element shelfmark = doc.createElement("shelfmark");
            bibliography.appendChild(shelfmark);
            shelfmark.appendChild(doc.createTextNode(metadata.getShelfmark()));

//            Element measure = doc.createElement("measure");
//            bibliography.appendChild(measure);
//            measure.appendChild(doc.createTextNode(metadata.get))
        }

        write(doc, out);
    }

    /**
     * @return a new DOM document
     */
    private Document newDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }

        return builder.newDocument();
    }

    /**
     * @param doc document
     * @param out output stream
     */
    private void write(Document doc, OutputStream out) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // Options to make it human readable
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
        } catch (TransformerConfigurationException e) {
            return;
        }

        Source xmlSource = new DOMSource(doc);
        Result result = new StreamResult(out);

        try {
            transformer.transform(xmlSource, result);
        } catch (TransformerException e) {
            return;
        }
    }

    private Map<String, BookMetadata> buildMetadata(Document doc) {
        Map<String, BookMetadata> metadataMap = new HashMap<>();

        Element top = doc.getDocumentElement();
//  TODO support lang

        for (String lang : getLanguages(doc)) {
            BookMetadata metadata = new BookMetadata();

            metadata.setNumberOfIllustrations(Integer.parseInt(top.getAttribute("illustrations")));
            metadata.setNumberOfPages(Integer.parseInt(top.getAttribute("pages")));

            NodeList list = top.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element el = (Element) node;
                if (el.getNodeName().equals("dimensions")) {
                    metadata.setHeight(Integer.parseInt(el.getAttribute("height")));
                    metadata.setWidth(Integer.parseInt(el.getAttribute("width")));
                } else if (el.getNodeName().equals("date")) {
                    metadata.setYearStart(Integer.parseInt(el.getAttribute("start")));
                    metadata.setYearEnd(Integer.parseInt(el.getAttribute("end")));
                    metadata.setDate(el.getTextContent());
                } else if (el.getNodeName().equals("texts")) {
                    List<BookText> texts = handleBookTexts(el);
                    metadata.setTexts(texts.toArray(new BookText[texts.size()]));
                } else if (el.getNodeName().equals("bibliography") && el.getAttribute("lang").equals(lang)) {
                    readBibInfo(el, metadata);
                }
            }

            metadataMap.put(lang, metadata);
        }

        return metadataMap;
    }

    private String[] getLanguages(Document doc) {
        List<String> langs = new ArrayList<>();

        NodeList bibs = doc.getElementsByTagName("bibliography");
        for (int i = 0; i < bibs.getLength(); i++) {
            Node node = bibs.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            String lang = child.getAttribute("lang");

            if (lang != null && !lang.equals("")) {
                langs.add(lang);
            }
        }

        return langs.toArray(new String[langs.size()]);
    }

    private void readBibInfo(Element biblio, BookMetadata metadata) {
        NodeList children = biblio.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) child;

            String name = el.getNodeName();
            String content = el.getTextContent();
            switch (name) {
                case "type":
                    metadata.setType(content);
                    break;
                case "commonName":
                    metadata.setCommonName(content);
                    break;
                case "material":
                    metadata.setMaterial(content);
                    break;
                case "origin":
                    metadata.setOrigin(content);
                    break;
                case "currentLocation":
                    metadata.setCurrentLocation(content);
                    break;
                case "repository":
                    metadata.setRepository(content);
                    break;
                case "shelfmark":
                    metadata.setShelfmark(content);
                    break;
                case "measure":
                    break;
                case "date":
                    metadata.setDate(content);
                    metadata.setYearStart(Integer.parseInt(el.getAttribute("start")));
                    metadata.setYearEnd(Integer.parseInt(el.getAttribute("end")));
                    break;
                default:
                    break;
            }
        }
    }

    private List<BookText> handleBookTexts(Element el) {
        List<BookText> texts = new ArrayList<>();

        NodeList list = el.getElementsByTagName("text");
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element item = (Element) list.item(i);
            BookText text = new BookText();

            text.setId(item.getAttribute("id"));
            text.setTitle(item.getAttribute("title"));
            text.setFirstPage(item.getAttribute("start"));
            text.setLastPage(item.getAttribute("end"));
            text.setNumberOfPages(Integer.parseInt(item.getAttribute("pages")));
            text.setNumberOfIllustrations(Integer.parseInt(item.getAttribute("illustrations")));
            text.setLinesPerColumn(Integer.parseInt(item.getAttribute("linesPerColumn")));
            text.setLeavesPerGathering(Integer.parseInt(item.getAttribute("leavesPerGathering")));
            text.setColumnsPerPage(Integer.parseInt(item.getAttribute("columnsPerPage")));

            texts.add(text);
        }

        return texts;
    }
}