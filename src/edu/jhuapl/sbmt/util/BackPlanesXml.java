package edu.jhuapl.sbmt.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.jhuapl.sbmt.util.BackPlanesXmlMeta.MetaField;

import altwg.Fits.HduTags;
import altwg.XML.XmlDoc;
import altwg.XML.XmlDocFactory;
import nom.tam.fits.HeaderCard;


/**
 * Handles the creation, modification, and writing of PDS4 XML files describing
 * the BackPlane images. Assumes images are stored as FITS image cubes.
 *
 * @author espirrc1
 *
 */
public class BackPlanesXml
{

    public XmlDoc xmlDoc;

    // String pipePath;

    /**
     * Use XML Document Object Model to read, parse, and update XML. MetaData is
     * used to determine which XML template to load and convert into XML
     * Document object.
     *
     * @param metaData
     */
    public BackPlanesXml(BackPlanesXmlMeta metaData, String xmlTemplate)
    {
        xmlDoc = XmlDocFactory.getXmlDoc(xmlTemplate);

        // update the XML template, populating tags with metaData
        updateImgXml(xmlDoc, metaData);

    }

    /**
     * method used by this class to update the FITS Image XMl template document
     * with information from the metaData object.
     *
     * @param metaData
     */
    public void updateImgXml(XmlDoc xmlDoc, BackPlanesXmlMeta metaData)
    {

        // add <Array_2D_Image>, 1 per FITS header tag that
        // contains PLANE<X> type of string.
        /**
         * For now the following portion is commented out, as it assumes the XML planes
         * are hardcoded relative to the subclass of PerspectiveImage. For example,
         * MSIImage will always have the same number of planes in the same order
         * for all it's XML labels. Thus, the <Array_2D_Image> objects are already
         * predefined in the xml template file hardcoded in the MSIImage class.
         */
//        try
//        {
//            add2dPlanes(xmlDoc, metaData.fitsHdrCards);
//        }
//        catch (SAXException e)
//        {
//            System.out.println(
//                    "SAXException: ERROR adding XML describing 2D image planes for fits file.");
//            e.printStackTrace();
//            System.exit(1);
//        }
//        catch (IOException e)
//        {
//            System.out.println(
//                    "IOException: ERROR adding XML describing 2D image planes for fits file.");
//            e.printStackTrace();
//            System.exit(1);
//        }

        Element docElem = xmlDoc.doc.getDocumentElement();

        /*
         * POPULATE THE FIELDS THAT ARE STANDARD IN EVERY PDS4 XML LABEL that
         * describes a FITS file.
         */
        //logical identifier
        NodeList myElemList = docElem.getElementsByTagName("logical_identifier");
        Node myElem = myElemList.item(0);
        myElem.setTextContent(metaData.metaStrings.get(MetaField.LOGICALID));

        // start_date_time
        myElemList = docElem.getElementsByTagName("start_date_time");
        myElem = myElemList.item(0);
        String pdsTime = metaData.metaStrings.get(MetaField.STARTDATETIME) + "Z";
        myElem.setTextContent(pdsTime);

        // stop_date_time
        myElemList = docElem.getElementsByTagName("stop_date_time");
        myElem = myElemList.item(0);
        pdsTime = metaData.metaStrings.get(MetaField.STOPDATETIME) + "Z";
        myElem.setTextContent(pdsTime);

        // file_name
        myElemList = docElem.getElementsByTagName("file_name");
        myElem = myElemList.item(0);
        myElem.setTextContent(metaData.metaStrings.get(MetaField.PRODUCTFILENAME));

        // number of lines in FITS image (assumed the same among multiple
        // planes)
        setLinesSamples(xmlDoc.doc, metaData.lines, 0);

        // number of samples in FITS image (assumed the same among multiple
        // planes)
        setLinesSamples(xmlDoc.doc, metaData.samples, 1);

        // set image offset for each of the 2D planes.
        setImageOffsets(xmlDoc.doc, metaData);

        // set number of bytes in FITS header
        String value = String.valueOf(metaData.headerSize);
        String parentName = "Header";
        String childName = "object_length";
        XmlDoc.setChildbyParent(xmlDoc.doc, parentName, childName, value);

        long moe = metaData.headerSize;
        System.out.println("headerSize is:" + String.valueOf(moe));

    }

    public void writeXML(String outXmlFname)
            throws TransformerException, XPathExpressionException
    {

        // write the content into xml file
        Document doc = xmlDoc.doc;

        // remove whitespace first
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate(
                "//text()[normalize-space()='']", doc, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i)
        {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outXmlFname));
        transformer.transform(source, result);

        System.out.println("Done writing to:" + outXmlFname);

    }

    /**
     * Set Line or Sample 'elements' tags to the same value. It assumes the
     * images in the FITS file are all the same size, i.e. 1 or more image
     * planes where every plane has the same number of Lines and Samples.
     *
     * @param doc
     *            Document object
     * @param numValue
     *            value to set for Line or Sample
     * @param whichAxis
     *            =0 set Line, =1 set Sample
     */
    public static void setLinesSamples(Document doc, int numValue,
            int whichAxis)
    {

        String value = String.valueOf(numValue);
        String axisValue;
        if (whichAxis == 0)
        {
            axisValue = "Line";
        }
        else
        {
            axisValue = "Sample";
        }

        /**
         * 'elements' is the value we want to set, but only in cases where the
         * first child of the parent of 'elements' is called 'axis_name' and
         * where the value of 'axis_name' is "Line" or "Sample" depending on
         * whichAxis was set.
         */
        NodeList myElemList = doc.getElementsByTagName("elements");
        for (int ii = 0; ii < myElemList.getLength(); ii++)
        {
            Node node = myElemList.item(ii);
            Node parent = node.getParentNode();

            /*
             * set a child of the parent node. this assumes we want to set all
             * the children of the parent node that match the child name.
             */
            String childName = "elements";
            String siblingName = "axis_name";
            XmlDoc.setChildbySiblingNameVal(parent, siblingName, axisValue,
                    childName, value);

        }
    }

    /**
     * Set the value of an <lidvid_reference> child of the <Internal_Reference> tag. There are potentially many
     * instances of <lidvid_reference> so the caller must also specify the associated <reference_type> to narrow
     * down the possible choices.
     *
     * For example, in order to add an lidvid reference to the source calibrated data product which was used
     * to generate the product being referenced by the XML file in File_Area_Observational.File.file_name.
     * set lidvidRef = "<source file name>" and refType = "data_to_calibrated_product".
     * @param doc
     * @param reference
     * @param refType
     * @param refVal
     */
    public void setLidvidReference(String lidvidRef, String refType) {

        NodeList myElemList = xmlDoc.doc.getElementsByTagName("Internal_Reference");
        for (int ii = 0; ii < myElemList.getLength(); ii++) {
            Node parentNode = myElemList.item(ii);
            String refTypeTag = "reference_type";
            String lidvidTag = "lidvid_reference";
            XmlDoc.setChildbySiblingNameVal(parentNode, refTypeTag, refType, lidvidTag, lidvidRef);
        }

    }

    /**
     * Set the image offsets for each of the 2D image planes in the fits file.
     * The offset consists of the offset from the fits header plus the offset
     * from the previous image plane. Formula is: img offset = (hdr size) +
     * (lines * samples * bytes per pix * plane index) where plane index starts
     * at 0 and increments through each image plane. Note for the first image
     * plane the offset is just the offset from the fits header.
     *
     * @param doc
     * @param metaData
     */
    public static void setImageOffsets(Document doc, BackPlanesXmlMeta metaData)
    {

        // assume this gets the image planes in the same order as in the
        // document.
        NodeList myElemList = doc.getElementsByTagName("Array_2D_Image");
        String childName = "offset";

        for (int ii = 0; ii < myElemList.getLength(); ii++)
        {
            Node imgNode = myElemList.item(ii);
            String offsetVal = String.valueOf(metaData.imageOffsets.get(ii));
            XmlDoc.setChildNode(imgNode, childName, offsetVal);
        }
    }

    /**
     * Add XML elements describing the FITS 2D image planes. Elements <name>,
     * <description>, and <unit> will be populated with information parsed from
     * fits headers (captured in List<HeaderCard>).
     *
     * @param xmlDoc
     * @param headerCards
     * @throws SAXException
     * @throws IOException
     */
    public static void add2dPlanes(XmlDoc xmlDoc, List<HeaderCard> headerCards)
            throws SAXException, IOException
    {

        Document doc = xmlDoc.doc;

        // need to filter the headerCards to make a new list that only contains
        // fits tags matching "PLANE<X>"
        List<HeaderCard> planeCards = new ArrayList<HeaderCard>();
        for (HeaderCard hdrCard : headerCards)
        {
            String fitsKey = hdrCard.getKey();
            /*
             * if (HduTags.has(fitsKey)) { if
             * (PlaneInfo.first6HTags.contains(HduTags
             * .valueOf(fitsKey.toUpperCase()))) { planeCards.add(hdrCard); } }
             */
        }

        NodeList docNodeList = doc
                .getElementsByTagName("File_Area_Observational");
        Node fao = docNodeList.item(0);

        for (int ii = 0; ii < planeCards.size(); ii++)
        {
            addXmlArray2D(xmlDoc, fao);
        }

        // now populate the xml elements
        NodeList myElemList = doc.getElementsByTagName("Array_2D_Image");
        String childName;
        String childVal;
        for (int ii = 0; ii < myElemList.getLength(); ii++)
        {
            Node imgNode = myElemList.item(ii);
            HeaderCard fitsHeader = planeCards.get(ii);

            // set <name>
            childName = "name";
            childVal = fitsHeader.getKey();
            // String offsetVal = String.valueOf((metaData.lines *
            // metaData.samples * 4 * ii));
            XmlDoc.setChildNode(imgNode, childName, childVal);

            // set <description>
            childName = "description";
            childVal = fitsHeader.getValue();
            XmlDoc.setChildNode(imgNode, childName, childVal);

            // parse name and add <unit> if name matches enumeration.
            addGravXmlUnits(doc, imgNode, fitsHeader.getKey());
        }

    }

    /**
     * Create a new PDS4 <Array_2D_Image> element and add it as a child to the
     * given node. Will only populate element with name and description of
     * array.
     *
     * @param doc
     * @param key
     * @param value
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public static void addXmlArray2D(XmlDoc xmlDoc, Node parentNode)
            throws SAXException, IOException
    {

        // hardcoded blank XML fragment.
        String array2dImage = "\n" + "<Array_2D_Image>\n" + "\t<name></name>\n"
                + "\t<offset unit=\"byte\"></offset>\n" + "\t<axes>2</axes>\n"
                + "\t<axis_index_order>Last Index Fastest</axis_index_order>\n"
                + "\t<description>/</description>\n" + "\t<Element_Array>\n"
                + "\t\t<data_type>IEEE754MSBSingle</data_type>\n"
                + "\t</Element_Array>\n" + "\t<Axis_Array>\n"
                + "\t\t<axis_name>Line</axis_name>\n"
                + "\t\t<elements></elements>\n"
                + "\t\t<sequence_number>1</sequence_number>\n"
                + "\t</Axis_Array>\n" + "\t<Axis_Array>\n"
                + "\t\t<axis_name>Sample</axis_name>\n"
                + "\t\t<elements></elements>\n"
                + "\t\t<sequence_number>2</sequence_number>\n"
                + "\t</Axis_Array>\n" + "</Array_2D_Image>";

        // Node array2dNode = makeFragmentNode(xmlDoc, array2dImage);
        // parentNode.appendChild(array2dNode);

        appendXmlFragment(xmlDoc, parentNode, array2dImage);

    }

    public static Node addTextNode(Document doc, String key, String value,
            String attrib, String attribVal)
    {
        Element node = doc.createElement(key);
        node.appendChild(doc.createTextNode(value));
        if (attrib.length() > 0)
        {
            node.setAttribute(attrib, attribVal);
        }
        return node;
    }

    /**
     * Add an <External Reference> to a parent Node. The description text is optional and <description> tag will not be created if it is empty.
     * Example <External_Reference>:
     *
     * <External_Reference>
     *   <description>This is another external reference</description>
     *   <reference_text>This is the main text of the reference</reference_text>
     * </External_Reference>
     * @param parentNode
     * @param desc
     * @param refText
     */
    public void addExternalRef(XmlDoc xmlDoc, Node parentNode, String desc, String refText) {

        Document doc = xmlDoc.doc;

//        Element dataTag = doc.getDocumentElement();
//        Element parentTag =  (Element) dataTag.getElementsByTagName("people").item(0);
//
//        Element newPerson = doc.createElement("person");
//
//        Element firstName = doc.createElement("firstName");
//        firstName.setTextContent("Tom");
//
//        Element lastName = doc.createElement("lastName");
//        lastName.setTextContent("Hanks");

//        newPerson.appendChild(firstName);
//        newPerson.appendChild(lastName);
//
//        peopleTag.appendChild(newPerson);

        Element newRef = doc.createElement("External_Reference");
        if (desc.length() > 0) {
            Element descTag = doc.createElement("description");
            descTag.setTextContent(desc);
            newRef.appendChild(descTag);
        }
        Element refTag = doc.createElement("reference_text");
        refTag.setTextContent(refText);
        newRef.appendChild(refTag);
        parentNode.appendChild(newRef);

    }


    /**
     * Parse the plane name to determine whether it matches any of the
     * enumeration. If so then add xml <units> to Node.
     *
     * @param parentNode
     * @param description
     */
    public static void addGravXmlUnits(Document doc, Node parentNode,
            String planeName)
    {
        if (HduTags.has(planeName))
        {
            // match found
            String units = HduTags.valueOf(planeName.toUpperCase()).unit();
            if (units.length() > 0)
            {
                // units not null
                // find appropriate node
                if (parentNode instanceof Element)
                {
                    Element docElement = (Element) parentNode;
                    NodeList myElemList = docElement
                            .getElementsByTagName("Element_Array");

                    // there should be only 1 instance of <Element_Array>
                    Node thisNode = myElemList.item(0);
                    Element unitNode = doc.createElement("units");
                    unitNode.appendChild(doc.createTextNode(units));
                    thisNode.appendChild(unitNode);

                }
                else
                {
                    return;
                }
            }

        }
    }

    /**
     * Adds a string XML fragment to an existing XML node.
     *
     * @param docBuilder
     *            the parser
     * @param parent
     *            node to add fragment to
     * @param fragment
     *            a well formed XML fragment
     */
    public static void appendXmlFragment(XmlDoc xmlDoc, Node parent,
            String fragment) throws IOException, SAXException
    {
        DocumentBuilder docBuilder = xmlDoc.db;
        Document doc = parent.getOwnerDocument();
        Node fragmentNode = docBuilder
                .parse(new InputSource(new StringReader(fragment)))
                .getDocumentElement();
        fragmentNode = doc.importNode(fragmentNode, true);
        parent.appendChild(fragmentNode);
    }

}
