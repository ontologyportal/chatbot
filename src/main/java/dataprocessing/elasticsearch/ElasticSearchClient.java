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

    /** ***************************************************************
     * Constructor
     */
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

    /** ***************************************************************
     * Closes client connection
     */
    public void close() {

        try {
            client.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * @param corpus Corpus line is from
     * @param line Line number in corpus
     * @param text line text
     * Indexes line from corpus
     *
     * Mapping for chatbot/dialog:
         PUT chatbot
         {
             "mappings": {
                 "dialog": {
                     "properties": {
                         "corpus": {
                             "type": "string",
                                     "index": "not_analyzed"
                         },
                         "file": {
                             "type": "string",
                                     "index": "not_analyzed"
                         },
                         "line": {
                             "type": "long",
                                     "index": "not_analyzed"
                         },
                         "text": {
                             "type": "string",
                                     "index": "analyzed",
                                     "analyzer": "english"
                         }
                     }
                 }
             }
         }
     */
     public void indexDocument(String corpus, String file, int line, String text) {

         try {
             JSONObject entity = new JSONObject();
             entity.put("corpus", corpus);
             entity.put("file", file);
             entity.put("line", line);
             entity.put("text", text);

            // Post JSON entity
             client.performRequest("POST",
                     String.format("/%s/%s/%s", index, type, corpus + "_" + file + "_" + line),
                     Collections.emptyMap(),
                     new NStringEntity(entity.toString(), ContentType.APPLICATION_JSON),
                     header);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * @param id Id of document to retrieve
     * @return document in JSON
     */
    public JSONObject retrieveDocument(String id) {

        JSONObject documents = new JSONObject();

        try {
            // Get raw response
            Response response = client.performRequest("GET", String.format("/%s/%s/%s", index, type, id), header);

            // Convert raw response into JSON
            String str;
            StringBuilder responseStrBuilder = new StringBuilder();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((str = streamReader.readLine()) != null) responseStrBuilder.append(str);

            // Extract documents
            if (id == null || id.equals(""))
                documents = (JSONObject) ((JSONObject) new JSONObject(responseStrBuilder.toString()).get("hits")).get("hits");
            else
                documents = (JSONObject) new JSONObject(responseStrBuilder.toString()).get("_source");

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return documents;
    }

    /** ***************************************************************
     * @param id Id of document to delete
     * Deletes document from index
     */
    public void deleteDocument(String id) {

        try {
            client.performRequest("Delete", String.format("/%s/%s/%s", index, type, id), header);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
