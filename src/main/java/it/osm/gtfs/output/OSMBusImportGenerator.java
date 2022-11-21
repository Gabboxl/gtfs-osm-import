/**
   Licensed under the GNU General Public License version 3
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.gnu.org/licenses/gpl-3.0.html

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

**/
package it.osm.gtfs.output;

import it.osm.gtfs.model.BoundingBox;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OSMBusImportGenerator implements IElementCreator{
	private boolean completed = false;
	private Document document;
	private Element root;

	public OSMBusImportGenerator(BoundingBox bb) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation impl = builder.getDOMImplementation();

		document = impl.createDocument(null,null,null);

		root = document.createElement("osm");
		root.setAttribute("version", "0.5");
		root.setAttribute("generator", "GTFSImport");
		document.appendChild(root);
		root.appendChild(bb.getXMLTag(this));
	}

	public void end() {
		if (completed)
			throw new IllegalArgumentException("This buffer is already closed.");
		completed = true;
	}

	public void saveTo(OutputStream outputStream) throws IOException, TransformerException {
		if (!completed)
			throw new IllegalArgumentException("This buffer isn't complete, can't save.");
		
		Source source = new DOMSource(document);
		Result result = new StreamResult(outputStream);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(source, result);
	}

	public void appendNode(Element n) throws TransformerException {
		if (completed)
			throw new IllegalArgumentException("This buffer is already closed.");
		root.appendChild(document.importNode(n, true));
	}

	@Override
	public Element createElement(String tagName) {
		if (completed)
			throw new IllegalArgumentException("This buffer is already closed.");
		return document.createElement(tagName);
	}
}
