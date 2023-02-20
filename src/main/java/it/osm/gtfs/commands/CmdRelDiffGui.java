/**
 * Licensed under the GNU General Public License version 3
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package it.osm.gtfs.commands;

import it.osm.gtfs.commands.gui.GTFSRouteDiffGui;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "reldiffx", mixinStandardHelpOptions = true, description = "Analyze the diff between osm relations and gtfs trips (GUI)")
public class CmdRelDiffGui implements Callable<Void> {

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException {
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

        synchronized (lock) {
            while (app.isVisible())
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        app.dispose();
        System.out.println("Done");

        return null;
    }

}
