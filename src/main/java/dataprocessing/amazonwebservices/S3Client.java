package dataprocessing.amazonwebservices;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
 * Created by charlescostello on 1/30/17.
 * Class to wrap the S3 API
 */
public class S3Client {

    private AmazonS3 client;
    private String bucket;

    /** *************************************************************
     * Constructor
     */
    public S3Client() {

        ResourceBundle resourceBundle = ResourceBundle.getBundle("aws");
        bucket = resourceBundle.getObject("bucket").toString();
        client = new AmazonS3Client();
    }

    /** *************************************************************
     * @param directory Directory of files to be returns
     * @return List of files in directory
     * Retrieves list of files in given directory
     */
    public List<String> getDirectoryFiles(String directory) {

        List<String> files = new ArrayList<>();

        try {
            files = client.listObjects(bucket, directory).getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toCollection(ArrayList::new));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return files;
    }


    /** *************************************************************
     * @param filename name of file to retrieve from s3
     * @return file name of local file
     * Gets file from S3 and writes to local file
     */
    public List<String> readS3File(String filename) {

        List<String> lines = new ArrayList<>();

        try {
            S3Object object = client.getObject(bucket, filename);
            S3ObjectInputStream stream = object.getObjectContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while((line = bufferedReader.readLine()) != null)
                lines.add(line);

            bufferedReader.close();
            stream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return lines;
    }
}