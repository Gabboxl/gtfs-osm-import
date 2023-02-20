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
package it.osm.gtfs.utils;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/***
 * This class contains methods to download files
 */
public class DownloadUtils {
    private static final int TIMEOUT = 30 * 60000;

    public static void download(String url, File dest, boolean useGzipCompression) throws IOException {
        int currentretry = 0;
        final int MAX_RETRIES = 3;

        while (++currentretry <= MAX_RETRIES) {
            System.out.println("Downloading (retry count: " + currentretry + "/" + MAX_RETRIES + "): " + url);
            try {
                //System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                if (useGzipCompression) {
                    conn.setRequestProperty("Accept-Encoding", "gzip");
                }

                //conn.setConnectTimeout(TIMEOUT);
                //conn.setReadTimeout(TIMEOUT);
                conn.setRequestMethod("GET");
                //System.out.println(conn.getRequestProperties());

                InputStream in = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(dest);
                BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                byte[] data = new byte[1024];

                int x = 0;

                while ((x = in.read(data, 0, 1024)) >= 0) {
                    bout.write(data, 0, x);
                }

                bout.close();
                in.close();

                return;
            } catch (SocketTimeoutException | ConnectException e) {
                e.printStackTrace();
            }
        }
        throw new SocketTimeoutException();
    }

    public static void downloadZip(String zipUrl, String extractDir) {
        try {
            URL url = new URL(zipUrl);
            ZipUtils.unzipToDirectory(url.openStream(), extractDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
