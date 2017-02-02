package dataprocessing.elasticsearch;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.ResourceBundle;

/**
 * This code is copyright CloudMinds 2017.
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * and Teknowledge in any writings, briefings, publications, presentations, or
 * other representations of any software which incorporates, builds on, or uses this
 * code.  Please cite the following article in any publication with references:
 * Pease, A., (2003). The Sigma Ontology Development Environment,
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.
 *
 * Created by charlescostello on 2/1/17.
 * Class to wrap the Elasticsearch API
 */
public class ElasticSearchClient {

    private RestClient client;
    private Header header;
    private String index;
    private String type;

    public ElasticSearchClient() {

        ResourceBundle resourceBundle = ResourceBundle.getBundle("elasticsearch");
        index = resourceBundle.getObject("index").toString();
        type = resourceBundle.getObject("type").toString();
        header = new BasicHeader("CloudMinds","empty");
        client = RestClient.builder(new HttpHost(
                resourceBundle.getObject("host").toString(),
                Integer.parseInt(resourceBundle.getObject("port").toString()),
                resourceBundle.getObject("protocol").toString())).build();
    }

    public void close() {

        try {
            client.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void indexDocument() {

        try {
            JSONObject entity = new JSONObject();
            entity.put("corpora", "Test corpora");
            entity.put("line", "99");
            entity.put("text", "Test text and then some...");

            client.performRequest("PUT",
                    String.format("/%s/%s/1", index, type),
                    Collections.emptyMap(),
                    new NStringEntity(entity.toString(), ContentType.APPLICATION_JSON),
                    header);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject retrieveDocument() {

        JSONObject documents = new JSONObject();

        try {
            // Get raw response
            Response response = client.performRequest("GET", String.format("/%s/%s/1", index, type), header);

            // Convert raw response into JSON
            String str;
            StringBuilder responseStrBuilder = new StringBuilder();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((str = streamReader.readLine()) != null) responseStrBuilder.append(str);

            // Extract documents
            documents = (JSONObject) new JSONObject(responseStrBuilder.toString()).get("_source");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return documents;
    }
}
