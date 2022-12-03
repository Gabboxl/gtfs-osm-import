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
package it.osm.gtfs.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class DownloadUtils {
    private static final int TIMEOUT = 30*60000;

    public static void download(String url, File dest) throws MalformedURLException, IOException{
        int retry = 0;
        while (++retry <= 3){
            System.out.println("Downloading " + url + " Retry count: " + retry);
            try{
                //System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

                HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
                //conn.setConnectTimeout(TIMEOUT);
                //conn.setReadTimeout(TIMEOUT);
                conn.setRequestMethod("GET");
                //conn.addRequestProperty("Host", "overpass-api.de");
                //conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                //System.out.println(conn.getRequestProperties());


                InputStream in = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(dest);
                BufferedOutputStream bout = new BufferedOutputStream(fos,1024);
                byte[] data = new byte[1024];
                int x=0;
                while((x=in.read(data,0,1024))>=0){
                    bout.write(data,0,x);
                }
                bout.close();
                in.close();
                return;
            }catch(SocketTimeoutException | ConnectException e){
                System.err.println(e);
            }
        }
        throw new SocketTimeoutException();
    }
}
