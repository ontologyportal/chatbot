package dataprocessing.amazonwebservices;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

    /** *************************************************************
     * @param filename name of file to retrieve from s3
     * @return file name of local file
     * Gets file from S3 and writes to local file
     */
    public static String readS3File(String filename) {

        String newFileName = "testFile.txt";
        String bucketName = "cloudminds-nlp";
        AmazonS3 client = new AmazonS3Client();
        try {
            S3Object object = client.getObject(bucketName, filename);
            S3ObjectInputStream contentStream = object.getObjectContent();
            Files.copy(contentStream, new File(newFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return newFileName;
    }
}