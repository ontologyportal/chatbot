package chatbot;

import nlp.TFIDF;

import java.io.IOException;

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
 * Class to run the chatbot
 */
public class Chatbot {

    public static void main(String[] args) {

        try {
            String[] TFIDFArgs = new String[2];
            TFIDFArgs[0] = "-d";
            TFIDFArgs[1] = "-s";
            TFIDF.main(TFIDFArgs);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Test");
    }
}