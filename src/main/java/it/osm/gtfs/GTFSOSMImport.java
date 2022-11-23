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

package it.osm.gtfs;

import it.osm.gtfs.command.gui.GTFSRouteDiffGui;
import it.osm.gtfs.command.GTFSCheckOsmRoutes;
import it.osm.gtfs.command.GTFSGenerateBusStopsImport;
import it.osm.gtfs.command.GTFSGenerateGeoJSON;
import it.osm.gtfs.command.GTFSGenerateRoutesBaseRelations;
import it.osm.gtfs.command.GTFSGenerateRoutesDiff;
import it.osm.gtfs.command.GTFSGenerateRoutesGPXs;
import it.osm.gtfs.command.GTFSGenerateSQLLiteDB;
import it.osm.gtfs.command.GTFSGetBoundingBox;
import it.osm.gtfs.command.GTFSUpdateDataFromOSM;
import it.osm.gtfs.utils.GTFSImportSetting;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import asg.cliche.CLIException;
import asg.cliche.Command;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;

public class GTFSOSMImport {
	
	@Command(description="Get the Bounding Box of the GTFS File and xapi links")
	public void bbox() throws IOException, ParserConfigurationException, SAXException, TransformerException {
		GTFSGetBoundingBox.run();
	}

	@Command(description="Check and validate OSM relations")
	public void check(String osmId) throws IOException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {
		GTFSCheckOsmRoutes.run(osmId);
	}
	
	@Command(description="Generate/update osm data from api server")
	public void update() throws IOException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {
		GTFSUpdateDataFromOSM.run();
	}
	
	@Command(description="Generate/update single relation from api server")
	public void updates(String relation) throws IOException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {
		GTFSUpdateDataFromOSM.run(relation);
	}
	
	@Command(description="Generate files to import bus stops into osm merging with existing stops")
	public void stops() throws IOException, ParserConfigurationException, SAXException, TransformerException {
		GTFSGenerateBusStopsImport.run(false);
	}
	
	@Command(description="Generate files to import bus stops into osm merging with existing stops (export to small file)")
	public void stops(Boolean smallFile) throws IOException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {
		GTFSGenerateBusStopsImport.run(smallFile);
	}

	@Command(description="Generate .gpx file for all GTFS Trips")
	public void gpx() throws IOException, ParserConfigurationException, SAXException {
		GTFSGenerateRoutesGPXs.run();
	}

	@Command(description="Generate the base relations (including only stops) to be used only when importing without any existing relation in osm")
	public void rels() throws IOException, ParserConfigurationException, SAXException {
		GTFSGenerateRoutesBaseRelations.run();
	}

	@Command(description="Analyze the diff between osm relations and gtfs trips")
	public void reldiff() throws IOException, ParserConfigurationException, SAXException {
		GTFSGenerateRoutesDiff.run();
	}
	
	@Command(description="Analyze the diff between osm relations and gtfs trips")
	public void reldiffx() throws IOException, ParserConfigurationException, SAXException {
		final Object lock = new Object();
		final GTFSRouteDiffGui app = new GTFSRouteDiffGui();
		
		app.setVisible(true); 
	    app.addWindowListener(new WindowAdapter() {

	        @Override
	        public void windowClosing(WindowEvent arg0) {
	            synchronized (lock) {
	                app.setVisible(false);
	                lock.notify();
	            }
	        }

	    });
	    
	    synchronized(lock) {
            while (app.isVisible())
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
	    app.dispose();
        System.out.println("Done");
	}

	@Command(description="Generate a sqlite db containg osm relations")
	public void sqlite() throws ParserConfigurationException, SAXException, IOException{
		GTFSGenerateSQLLiteDB.run();
	}	
	
	@Command(description="Generate a geojson file containg osm relations")
	public void geojson() throws ParserConfigurationException, SAXException, IOException, JSONException{
		GTFSGenerateGeoJSON.run();
	}

	@Command(description="Display current configuration")
	public String conf(){
		return "Current Configuration:\n" +
				"GTFS Path: " + GTFSImportSetting.getInstance().getGTFSPath() + "\n" +
				"OSM Path: " + GTFSImportSetting.getInstance().getOSMPath() + "\n" +
				"OUTPUT Path: " + GTFSImportSetting.getInstance().getOutputPath() + "\n" +
				"Operator: " + GTFSImportSetting.getInstance().getOperator() + "\n" +
				"RevisitedKey: " + GTFSImportSetting.getInstance().getRevisitedKey() + "\n" +
				"Plugin Class: " + GTFSImportSetting.getInstance().getPlugin().getClass().getCanonicalName() + "\n";
	}
	
	@Command(description="Display available commands")
	public String help(){
		StringBuilder buffer = new StringBuilder();
		buffer.append("Available commands:\n");
		for (Method method:this.getClass().getMethods()){
			for(Annotation annotation : method.getDeclaredAnnotations()){
				if(annotation instanceof Command){
					Command myAnnotation = (Command) annotation;
					buffer.append(method.getName() + "\t" + myAnnotation.description() + "\n");
				}
			}
		}
		buffer.append("exit\tExit from GTFSImport\n");
		return buffer.toString();
	}

	public static void main(String[] args) throws IOException, CLIException {
		initChecks();
		System.out.println("GTFS Import\n");
		Shell shell = ShellFactory.createConsoleShell("GTFSImport", "", new GTFSOSMImport());
		shell.processLine("conf");
		shell.processLine("help");
		shell.commandLoop();
		// ensure we close app at this point even with awt resourced leaked
		System.exit(0);
	}
	
	private static void initChecks(){
		if (Runtime.getRuntime().maxMemory() < 1000000000){
			throw new IllegalArgumentException("You need to configure JVM to allow al least 1GB ram usage.");
		}
	}
}
