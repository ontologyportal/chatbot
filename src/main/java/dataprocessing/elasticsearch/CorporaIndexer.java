package dataprocessing.elasticsearch;

import dataprocessing.amazonwebservices.S3Client;

import java.util.Arrays;
import java.util.List;

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
public class CorporaIndexer {

    private ElasticSearchClient elasticSearchClient;
    private S3Client s3Client;
    private List<String> corpora;

    /** ***************************************************************
     * @param corpus Name of corpus
     * Indexes each file in a corpus
     */
    private void indexCorpus(String corpus) {

//        s3Client.getDirectoryFiles(corpus)
//                .forEach(file -> s3Client.readS3File(file)
//                        .forEach(line -> elasticSearchClient
//                                .indexDocument(corpus, file, file.indexOf(line), line)));
//
        for (String file : s3Client.getDirectoryFiles(corpus)) {
            List<String> lines = s3Client.readS3File(file);
//            for (int i = 0; i < lines.size(); i++) {
//                 elasticSearchClient.indexDocument(corpus, file, i, lines.get(i));
//            }
        }
    }

    /** ***************************************************************
     * Runs program
     */
    private void indexCorpora() {

        elasticSearchClient = new ElasticSearchClient();
        s3Client = new S3Client();
        corpora = Arrays.asList("Corpora/CornellMovieDialogs", "Corpora/NPSChatCorpus", "Corpora/NUSSMSCorpus", "Corpora/SwitchboardDialogs");
        corpora.forEach(this::indexCorpus);
    }

    /** ***************************************************************
     * @param args Program arguments
     * Initializes program
     */
    public static void main(String[] args) {

        CorporaIndexer corporaIndexer = new CorporaIndexer();
        corporaIndexer.indexCorpora();
    }
}
