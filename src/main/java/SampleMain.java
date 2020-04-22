import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fujitsu.xml.xbrl.inlinexbrl.generator.FootnoteMapping;
import com.fujitsu.xml.xbrl.inlinexbrl.generator.FormatType;
import com.fujitsu.xml.xbrl.inlinexbrl.generator.InlineXBRLGenerator;
import com.fujitsu.xml.xbrl.inlinexbrl.generator.InlineXBRLMapping;
import com.fujitsu.xml.xbrl.inlinexbrl.generator.NonFractionMapping;
import com.fujitsu.xml.xbrl.inlinexbrl.generator.NonNumericMapping;
import com.fujitsu.xml.xbrl.xwand.common.Anchor;
import com.fujitsu.xml.xbrl.xwand.common.AnchorList;
import com.fujitsu.xml.xbrl.xwand.common.LinkModelList;
import com.fujitsu.xml.xbrl.xwand.common.ResourceLink;
import com.fujitsu.xml.xbrl.xwand.common.ResourceLinkModel;
import com.fujitsu.xml.xbrl.xwand.instance.Footnote;
import com.fujitsu.xml.xbrl.xwand.instance.FootnoteLink;
import com.fujitsu.xml.xbrl.xwand.instance.Instance;
import com.fujitsu.xml.xbrl.xwand.instance.InstanceElement;
import com.fujitsu.xml.xbrl.xwand.instance.InstanceElementFilter;
import com.fujitsu.xml.xbrl.xwand.instance.InstanceElementList;
import com.fujitsu.xml.xbrl.xwand.instance.Item;
import com.fujitsu.xml.xbrl.xwand.instance.NonNumericItem;
import com.fujitsu.xml.xbrl.xwand.instance.NumericItem;
import com.fujitsu.xml.xbrl.xwand.instance.UserElement;
import com.fujitsu.xml.xbrl.xwand.processor.XBRLProcessor;

/**
 *
 * All Right Reserved, (C) Fujitsu 2017
 */
public class SampleMain {

	public static void main(String[] args) throws Exception {

		if (args.length < 3){
			System.err.println("java SampleMain <XBRL File> <XHTML File> <IXBRL_1.0 File>");
			System.exit(1);
		}

		// Instance object of the target instance document
		XBRLProcessor proc = new XBRLProcessor();
		Instance instance = proc.loadInstance(new StreamSource(args[0]));

		// Document object of the target XHTML document
		Document xhtmlDoc = loadXHTML(args[1]);

		// create a mapping for fact #1
		NonNumericItem fact1 = getNonNumericItem(instance, "DocumentPeriodEndDate");
		Node dateNode = getFirstChildOfElement(xhtmlDoc, "date");
		NonNumericMapping mapping1 = new NonNumericMapping(
				fact1,
				dateNode,
				FormatType.DATE_LONG_US,
				false);

		// create a mapping for fact #2
		NumericItem fact2 = getNumericItem(instance, "Sales");
		Node salesNode = getFirstChildOfElement(xhtmlDoc, "sales");
		NonFractionMapping mapping2 = new NonFractionMapping(
				fact2,
				salesNode,
				FormatType.NUM_COMMA_DOT,
				6);

		// create a mapping for fact #3
		NumericItem fact3 = getNumericItem(instance, "NetIncomeLoss");
		Node incomeNode = getFirstChildOfElement(xhtmlDoc, "income");
		NonFractionMapping mapping3 = new NonFractionMapping(
				fact3,
				incomeNode,
				FormatType.NUM_COMMA_DOT,
				6);

		// create a mapping for fact #4
		NonNumericItem fact4 = getNonNumericItem(instance, "IncomeTaxDisclosureTextBlock");
		Node startNode = getFirstChildOfElement(xhtmlDoc, "textblock");
		Node endNode = getLastChildNodeOfElement(xhtmlDoc, "textblock");
		NonNumericMapping mapping4 = new NonNumericMapping(
				fact4,
				startNode,
				endNode,
				null,
				true);

		// create a mapping for footnote #5
		Footnote footnote5 = getFootnote(instance, "footnote5");
		Node footnoteNode = getFirstChildOfElement(xhtmlDoc, "footnote");
		FootnoteMapping mapping5 = new FootnoteMapping(
				footnote5,
				footnoteNode);

		// create an array of mappings
		InlineXBRLMapping[] mappings = new InlineXBRLMapping[] {
			mapping1, mapping2, mapping3, mapping4, mapping5
		};

		// create a inline XBRL generator
		InlineXBRLGenerator generator = new InlineXBRLGenerator();

		// generate inline XBRL
		Document inlineDoc = generator.generateInlineXBRL(instance, xhtmlDoc, mappings);

		TransformerFactory trf = TransformerFactory.newInstance();
		Transformer tr = trf.newTransformer();
		tr.transform(new DOMSource(inlineDoc), new StreamResult(args[2]));
	}

	private static Document loadXHTML(String path) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(path);
	}

	private static Footnote getFootnote(Instance instance, String label){
		FootnoteLink link = instance.getFootnoteLink();
		AnchorList anchors = link.getAnchors();
		for (int j = 0, nn = anchors.size(); j < nn; j++){
			Anchor a = anchors.get(j);
			if (a.getAnchorType() == Anchor.ANCHOR_TYPE_ELEMENT_DECL){
				continue;
			}
			UserElement ue = (UserElement)a;
			LinkModelList models = link.getResourceModels(ue, ResourceLink.FILTER_DO_NOTHING, null, null);
			for (int i = 0, n = models.size(); i < n; i++){
				ResourceLinkModel model = (ResourceLinkModel)models.get(i);
				if(model.getLinkInformation().getDestLocator().getLabel().equals(label)){
					return (Footnote)model.getResource();
				}
			}
		}

		return null;
	}

	private static NonNumericItem getNonNumericItem(Instance instance, final String name){
		InstanceElementList list = instance.getDescendantsAndSelf(new InstanceElementFilter(){
			public boolean accept(InstanceElement arg0) {
				if (arg0.getElementType() == InstanceElement.ELEMENT_TYPE_NON_NUMERIC_ITEM
						&& ((Item)arg0).getElementDecl().getName().equals(name)){
					return true;
				}
				return false;
			}
		});
		return (list.size() > 0) ? (NonNumericItem)list.get(0) : null;
	}

	private static NumericItem getNumericItem(Instance instance, final String name){
		InstanceElementList list = instance.getDescendantsAndSelf(new InstanceElementFilter(){
			public boolean accept(InstanceElement arg0) {
				if (arg0.getElementType() == InstanceElement.ELEMENT_TYPE_NUMERIC_ITEM
						&& ((Item)arg0).getElementDecl().getName().equals(name)){
					return true;
				}
				return false;
			}
		});
		return (list.size() > 0) ? (NumericItem)list.get(0) : null;
	}

	private static Node getFirstChildOfElement(Document doc, String id){
		Element e = getElementById(doc, id);
		if (e == null){
			return null;
		}
		return e.getFirstChild();
	}

	private static Node getLastChildNodeOfElement(Document doc, String id){
		Element e = getElementById(doc, id);
		if (e == null){
			return null;
		}
		return e.getLastChild();
	}

	private static Element getElementById(Document doc, String id){
		NodeList elements = doc.getElementsByTagName("*");
		for (int i = 0, n = elements.getLength(); i < n; i++){
			Element e = (Element)elements.item(i);
			if (id.equals(e.getAttribute("id"))){
				return e;
			}
		}
		return null;
	}
}
