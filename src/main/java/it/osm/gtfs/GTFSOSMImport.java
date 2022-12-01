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

import it.osm.gtfs.command.*;
import it.osm.gtfs.command.gui.GTFSRouteDiffGui;
import it.osm.gtfs.utils.GTFSImportSetting;
import org.json.JSONException;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

@CommandLine.Command(name = "GTFSOSMImport", subcommands = {
		GTFSUpdateDataFromOSM.class, GTFSGenerateBusStopsImport.class,
GTFSGetBoundingBox.class, GTFSGenerateRoutesGPXs.class, GTFSGenerateRoutesBaseRelations.class, GTFSGenerateRoutesFullRelations.class,
GTFSMatchGPX.class, GTFSCheckOsmRoutes.class,
		GTFSGenerateRoutesDiff.class
})
public class GTFSOSMImport {
	@CommandLine.Command(description = "Analyze the diff between osm relations and gtfs trips")
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


	@CommandLine.Command(description = "Display current configuration")
	String conf(){
		return "Current Configuration:\n" +
				"GTFS Path: " + GTFSImportSetting.getInstance().getGTFSPath() + "\n" +
				"OSM Path: " + GTFSImportSetting.getInstance().getOSMPath() + "\n" +
				"OUTPUT Path: " + GTFSImportSetting.getInstance().getOutputPath() + "\n" +
				"Operator: " + GTFSImportSetting.getInstance().getOperator() + "\n" +
				"RevisitedKey: " + GTFSImportSetting.getInstance().getRevisitedKey() + "\n" +
				"Plugin Class: " + GTFSImportSetting.getInstance().getPlugin().getClass().getCanonicalName() + "\n";
	}

	public static void main(String[] args) {
		initChecks();
		System.out.println("Welcome to GTFS-OSM-Import!\n");

		int exitCode = new CommandLine(new GTFSOSMImport()).execute(args);
		System.exit(exitCode);
	}
	
	private static void initChecks(){
		if (Runtime.getRuntime().maxMemory() < 1000000000){
			throw new IllegalArgumentException("You need to configure JVM to allow al least 1GB ram usage.");
		}
	}
}
