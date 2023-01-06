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

import it.osm.gtfs.commands.*;
import it.osm.gtfs.commands.gui.GTFSRouteDiffGui;
import it.osm.gtfs.utils.GTFSImportSettings;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;



@CommandLine.Command(name = "GTFSOSMImport", /* questo aggiunge le opzioni standard -h, -help, -V ecc */ mixinStandardHelpOptions = true, subcommands = {
        GTFSUpdateDataFromOSM.class, GTFSGenerateBusStopsImport.class,
        GTFSGetBoundingBox.class, GTFSGenerateRoutesGPXs.class, GTFSGenerateRoutesFullRelations.class,
        GTFSMatchGPXFile.class, GTFSCheckOsmRoutes.class,
        GTFSGenerateRoutesDiff.class
})
public class GTFSOSMImport {
    @CommandLine.Command(description = "Analyze the diff between osm relations and gtfs trips (GUI)")
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
    void conf(){
        System.out.println("Current Configuration:\n" +
                "GTFS path: " + GTFSImportSettings.getInstance().getGTFSDataPath() + "\n" +
                "Output path: " + GTFSImportSettings.getInstance().getOutputPath() + "\n" +
                "Operator: " + GTFSImportSettings.getInstance().getOperator() + "\n" +
                "Revised key: " + GTFSImportSettings.getInstance().getRevisedKey() + "\n" +
                "Plugin class: " + GTFSImportSettings.getInstance().getPlugin().getClass().getCanonicalName() + "\n");
    }


    @CommandLine.Command(description = {
            "Example interactive shell with completion and autosuggestions. " +
                    "Hit @|magenta <TAB>|@ to see available commands.",
            "Hit @|magenta ALT-S|@ to toggle tailtips.",
            ""},
            footer = {"", "Press Ctrl-D to exit."},
            subcommands = {
                    GTFSUpdateDataFromOSM.class, GTFSGenerateBusStopsImport.class,
                    GTFSGetBoundingBox.class, GTFSGenerateRoutesGPXs.class, GTFSGenerateRoutesFullRelations.class,
                    GTFSMatchGPXFile.class, GTFSCheckOsmRoutes.class,
                    GTFSGenerateRoutesDiff.class, PicocliCommands.ClearScreen.class, CommandLine.HelpCommand.class})
    static class CliCommands implements Runnable {
        PrintWriter out;

        CliCommands() {}

        public void setReader(LineReader reader){
            out = reader.getTerminal().writer();
        }

        public void run() {
            out.println(new CommandLine(this).getUsageMessage());
        }
    }



    //@CommandLine.Command(description = "Start GTFS-OSM-import in shell interactive mode")
    static void interactive(){
        try {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            // set up JLine built-in commands
            Builtins builtins = new Builtins(workDir, null, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            // set up picocli commands
            CliCommands commands = new CliCommands();

            PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
            // Or, if you have your own factory, you can chain them like this:
            // MyCustomFactory customFactory = createCustomFactory(); // your application custom factory
            // PicocliCommandsFactory factory = new PicocliCommandsFactory(customFactory); // chain the factories

            CommandLine cmd = new CommandLine(new GTFSOSMImport(), factory); //CommandLine cmd = new CommandLine(commands, factory); vecchia stringahel
            PicocliCommands picocliCommands = new PicocliCommands(cmd);

            Parser parser = new DefaultParser();
            try (Terminal terminal = TerminalBuilder.builder().build()) {
                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);

                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                        .build();
                builtins.setLineReader(reader);
                commands.setReader(reader);
                factory.setTerminal(terminal);
                TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
                widgets.enable();
                KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
                keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

                String prompt = "GTFSOSMImport> ";
                String rightPrompt = null;

                // start the shell and process input until the user quits with Ctrl-D
                String line;

                //show the help menu at startup of the interactive mode
                systemRegistry.execute("help");

                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // Ignore
                    } catch (EndOfFileException e) {
                        return;
                    } catch (Exception e) {
                        systemRegistry.trace(e);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    @CommandLine.Command(description = "Main command - it executes both the update & stops commands.")
    void start() throws IOException, ParserConfigurationException, InterruptedException, SAXException, TransformerException {

        //TODO: support command options of these two classes in the start command also / or use shared options i think
        new GTFSUpdateDataFromOSM().call();
        new GTFSGenerateBusStopsImport().call();

    }

    public static void main(String[] args) {
        initChecks();
        System.out.println("Welcome to GTFS-OSM-Import!\n");

        //the interactive mode is for internal use only (like testing multiple commands from a single IDE run without restarting the tool)
        if(args[0].equals("interactive")){
            interactive();
        } else {

            CommandLine commandLine = new CommandLine(new GTFSOSMImport());

            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
        }
    }

    private static void initChecks(){
        if (Runtime.getRuntime().maxMemory() < 1000000000){
            throw new IllegalArgumentException("You need to configure JVM to allow al least 1GB ram usage.");
        }
    }
}
