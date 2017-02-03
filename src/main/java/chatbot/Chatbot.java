package chatbot;

import com.articulate.sigma.DB;
import com.articulate.sigma.utils.ProgressPrinter;
import com.google.common.io.Resources;
import nlp.KMeans;
import nlp.TextFileUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ResourceBundle;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;

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

    // inverse document frequency = log of number of documents divided by
    // number of documents in which a term appears
    private HashMap<String,Float> idf = new HashMap<String,Float>();

    // number of documents in which a term appears
    private HashMap<String,Integer> docfreq = new HashMap<String,Integer>();

    // the length of a vector composed from each term frequency
    private HashMap<Integer,Float> euclid = new HashMap<Integer,Float>();

    // number of times a term appears in a document (where each document is an Integer index)
    private HashMap<Integer,HashMap<String,Integer>> tf = new HashMap<Integer,HashMap<String,Integer>>();

    // tf * idf (where each document is an Integer index)
    private HashMap<Integer,HashMap<String,Float>> tfidf = new HashMap<Integer,HashMap<String,Float>>();

    // similarity of each document to the query (index -1)
    private HashMap<Integer,Float> docSim = new HashMap<Integer,Float>();

    /** English "stop words" such as "a", "at", "them", which have no or little
     * inherent meaning when taken alone. */
    public ArrayList<String> stopwords = new ArrayList<String>();

    // each line of a corpus
    public ArrayList<String> lines = new ArrayList<String>();

    // use JUnit resource path for input file
    private static boolean asResource = false;

    // flag for development mode (use Scanner instead of console for input)
    private static boolean isDevelopment = false;

    // flag for excluding negative sentiment responses
    private static boolean isExcludingNegativeSentiment = false;

    // flag for choosing responses that match the question's sentiment
    private static boolean isMatchingSentiment = false;
    private Random rand = new Random();


    /** *************************************************************************************************
     * Constructor
     */
    public Chatbot(String stopwordsFilename) throws IOException {

        //System.out.println("Info in TFIDF(): Initializing");
        readStopWords(stopwordsFilename);
    }

    /** *************************************************************************************************
     * Constructor Overload
     */
    public Chatbot(List<String> documents, String stopwordsFilename) throws IOException {

        //System.out.println("Info in TFIDF(): Initializing");
        prepare(documents, stopwordsFilename);
    }

    /** *************************************************************************************************
     */
    public void prepare(List<String> documents, String stopwordsFilename) throws IOException {

        rand.setSeed(18021918); // Makes test results consistent
        readStopWords(stopwordsFilename);
        readDocuments(documents);
        calcIDF(documents.size());
        calcTFIDF();
    }

    /** ************************************************************************************************
     * Process a document
     * @param documents - list of strings to be processed
     */
    private void readDocuments(List<String> documents) {

        int count = 0;
        for (String doc : documents) {
            lines.add(doc);
            processDoc(doc, count);
            count++;
        }
    }

    /** ************************************************************************************************
     * inverse document frequency = log of number of documents divided by
     * number of documents in which a term appears.
     * Note that if the query is included as index -1 then it will
     * get processed too. Put the results into
     * HashMap<String,Float> idf
     */
    private void calcIDF(int docCount) {

        idf.putAll(docfreq.keySet().stream().collect(Collectors.toMap((t) -> t,t ->
                ((float) Math.log10((float) docCount / (float) docfreq.get(t))))));
    }

    /** ************************************************************************************************
     * Calculate TF/IDF and put the results in
     * HashMap<Integer,HashMap<String,Float>> tfidf
     * In the process, calculate the euclidean distance of the word
     * vectors and put in HashMap<Integer,Float> euclid
     * Note that if the query is included as index -1 then it will
     * get processed too.
     */
    private void calcOneTFIDF(Integer int1) {

        HashMap<String,Integer> tftermlist = tf.get(int1);
        if (tftermlist == null) {
            System.out.println("Error in calcOneTFIDF(): bad index: " + int1);
            return;
        }
        HashMap<String,Float> tfidflist = new HashMap<String,Float>();
        float euc = 0;
        Iterator<String> it2 = tftermlist.keySet().iterator();
        while (it2.hasNext()) {
            String term = it2.next();
            int tfint = tftermlist.get(term).intValue();
            float idffloat = idf.get(term).floatValue();
            float tfidffloat = idffloat * tfint;
            tfidflist.put(term,new Float(tfidffloat));
            euc = euc + (tfidffloat * tfidffloat);
        }
        euclid.put(int1, new Float((float) Math.sqrt(euc)));
        tfidf.put(int1, tfidflist);
    }



    /** *************************************************************************************************
     * Calculate TF/IDF and put results in
     * HashMap<Integer,HashMap<String,Float>> tfidf
     * Note that if the query is included as index -1 then it will
     * get processed too.
     * This calls calcOneTFIDF() that does most of the work.
     */
    private void calcTFIDF() {

        System.out.print("Info in TFIDF.calcTFIDF(): TF/IDF: ");
        ProgressPrinter pp = new ProgressPrinter(1000);
        tf.keySet().stream().forEach(s -> {calcOneTFIDF(s.intValue()); pp.tick();} );
        System.out.println();
    }

    /** ************************************************************************************************
     * sets the values in tf (term frequency) and tdocfreq (count of
     * documents in which a term appears)
     * @param intlineCount is -1 for query
     */
    private void processDoc(String doc, Integer intlineCount) {

        if (isNullOrEmpty(doc))
            return;
        String line = removePunctuation(doc);
        line = removeStopWords(line);
        if (isNullOrEmpty(line.trim()))
            return;
        ArrayList<String> tokens = splitToArrayList(line.trim());
        HashSet<String> tokensNoDup = new HashSet<String>();
        HashMap<String,Integer> tdocfreq = new HashMap<String,Integer>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            Integer tcount = new Integer(0);
            if (tdocfreq.containsKey(token))
                tcount = tdocfreq.get(token);
            int tcountint = tcount.intValue() + 1;
            tcount = new Integer(tcountint);
            tdocfreq.put(token,tcount);
            if (!docfreq.containsKey(token))
                docfreq.put(token,new Integer(1));
            else {
                if (!tokensNoDup.contains(token)) {
                    Integer intval = docfreq.get(token);
                    int intvalint = intval.intValue();
                    docfreq.put(token, new Integer(intvalint + 1));
                    tokensNoDup.add(token);
                }
            }
        }
        tf.put(intlineCount, tdocfreq);
    }

    /** **************************************************************************************************
     */
    protected void calcDFs() {

        System.out.println("Info in TFIDF.calcDFs(): Caclulate IDF, with size: " + lines.size());
        calcIDF(lines.size() - 1);
        calcTFIDF();
    }

    /** **************************************************************************************************
     * Assume that query is file index -1
     * Calculate the similarity of each document to the query
     * Put the result in HashMap<Integer,Float> docSim
     */
    private void calcDocSim() {

        //System.out.println("Info in TFIDF.calcDocSim(): tfidf: " + tfidf);
        Integer negone = new Integer(-1);
        HashMap<String,Float> tfidflist = tfidf.get(negone);
        HashMap<String,Float> normquery = new HashMap<String,Float>();
        float euc = euclid.get(negone);
        Iterator<String> it2 = tfidflist.keySet().iterator();
        while (it2.hasNext()) {
            String term = it2.next();
            float tfidffloat = tfidflist.get(term).floatValue();
            normquery.put(term,new Float(tfidffloat / euc));
        }
        //System.out.println("Info in TFIDF.calcDocSim(): normquery: " + normquery);
        Iterator<Integer> it1 = tf.keySet().iterator();
        while (it1.hasNext()) {
            Integer int1 = it1.next();
            if (int1.intValue() != -1) {
                tfidflist = tfidf.get(int1);
                euc = euclid.get(int1);
                float fval = 0;
                Iterator<String> it3 = tfidflist.keySet().iterator();
                while (it3.hasNext()) {
                    String term = it3.next();
                    float tfidffloat = tfidflist.get(term).floatValue();
                    float query = 0;
                    if (normquery.containsKey(term))
                        query = normquery.get(term).floatValue();
                    float normalize = 0;
                    if (euc != 0)
                        normalize = tfidffloat / euc;
                    fval = fval + (normalize * query);
                }
                docSim.put(int1,fval);
                //if (int1 == 8362)
                //    System.out.println("TFIDF.calcDocSim(): " + fval + ":" + tf.get(8362));
            }
        }
        //System.out.println("Info in TFIDF.calcDocSim(): Doc sim:\n" + docSim);
    }

    /** **************************************************************************************************
     * Remove punctuation and contractions from a sentence.
     * @return the sentence in a String minus these elements.
     */
    public String removePunctuation(String sentence) {

        Matcher m = null;
        if (isNullOrEmpty(sentence))
            return sentence;
        m = Pattern.compile("(\\w)\\'re").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'m").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)n\\'t").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'ll").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'s").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'d").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        m = Pattern.compile("(\\w)\\'ve").matcher(sentence);
        while (m.find()) {
            //System.out.println("matches");
            String group = m.group(1);
            sentence = m.replaceFirst(group).toString();
            m.reset(sentence);
        }
        sentence = sentence.replaceAll("\\'","");
        sentence = sentence.replaceAll("\"","");
        sentence = sentence.replaceAll("\\.","");
        sentence = sentence.replaceAll("\\;","");
        sentence = sentence.replaceAll("\\:","");
        sentence = sentence.replaceAll("\\?","");
        sentence = sentence.replaceAll("\\!","");
        sentence = sentence.replaceAll("\\, "," ");
        sentence = sentence.replaceAll("\\,[^ ]",", ");
        sentence = sentence.replaceAll("  "," ");
        return sentence;
    }

    /** **************************************************************************************************
     * Remove stop words from a sentence.
     * @return a string that is the sentence minus the stop words.
     */
    public String removeStopWords(String sentence) {

        if (isNullOrEmpty(sentence))
            return "";
        String result = "";
        ArrayList<String> al = splitToArrayList(sentence);
        if (al == null)
            return "";
        return al.stream()
                .filter(s -> !stopwords.contains(s.toLowerCase()))
                .collect(Collectors.joining(" "));
    }

    /** **************************************************************************************************
     * @return an ArrayList of the string split by spaces.
     */
    private static ArrayList<String> splitToArrayList(String st) {

        if (isNullOrEmpty(st)) {
            System.out.println("Error in TFIDF.splitToArrayList(): empty string input");
            return null;
        }
        String[] sentar = st.split(" ");
        return new ArrayList<String>(Arrays.asList(sentar)).stream()
                .filter(s -> s != null && !s.equals("") && !s.matches("\\s*"))
                .collect(Collectors.toCollection(ArrayList<String>::new));
    }

    /*****************************************************************************************************
     *
     * @param input
     * @return
     */
    public String matchBestInput(String input) {

        ArrayList<String> result = new ArrayList<>();
        TreeMap<Float,ArrayList<Integer>> sortedSim = matchInputFull(input);
        if (sortedSim == null || sortedSim.keySet().size() < 1 || sortedSim.lastKey() < .1) {
            return "I don't know";
        }
        Object[] floats = sortedSim.keySet().toArray();
        int numClusters = 3;
        if (floats.length < numClusters)
            numClusters = floats.length;
        float[] floatarray = new float[floats.length];
        for (int i = 0; i < floats.length; i++)
            floatarray[i] = (float) floats[i];
        ArrayList<ArrayList<Float>> res = KMeans.run(floatarray.length, floatarray, numClusters);
        ArrayList<Float> topCluster = res.get(res.size() - 2);
        while (res.get(res.size() - 2).size() > 3 && numClusters < floats.length) {
            numClusters++;
            res = KMeans.run(floatarray.length, floatarray, numClusters);
            topCluster = res.get(res.size() - 2);
            //System.out.println("Info in TFIDF.matchBestInput(): " + res);
            //System.out.println("Info in TFIDF.matchBestInput(): " + topCluster);
        }
        for (int i = 0; i < topCluster.size(); i++) {
            ArrayList<Integer> temp = sortedSim.get(topCluster.get(i));
            for (int j = 0; j < temp.size(); j++)
                result.add(lines.get(temp.get(j).intValue()));
        }

        ArrayList<String> resultNoProfanity = profanityFilter(result);

        ArrayList<String> rankedResponses = rankResponses(resultNoProfanity, input);

        return chooseBestResponse(rankedResponses);
    }

    /** ************************************************************************************************
     * Read a file of stopwords into the variable
     * ArrayList<String> stopwords
     */
    private void readStopWords(String stopwordsFilename) throws IOException {

        String filename = "";
        if (asResource) {
            URL stopWordsFile = Resources.getResource("resources/stopwords.txt");
            filename = stopWordsFile.getPath();
        }
        else
            filename = stopwordsFilename;
        FileReader r = new FileReader(filename);
        LineNumberReader lr = new LineNumberReader(r);
        String line;
        while ((line = lr.readLine()) != null)
            stopwords.add(line.intern());
        return;
    }

    /****************************************************************************************************
     *
     * @param first
     * @param second
     * @return
     */
    private boolean compareSentiment(int first, int second) {

        return first > 0 && second > 0 || first < 0 && second < 0 || first == 0 && second == 0;
    }

    /****************************************************************************************************
     *
     * @param responses
     * @param input
     * @return
     */
    private ArrayList<String> rankResponsesOnSentiment(ArrayList<String> responses, String input) {

        if (DB.sentiment.keySet().size() < 1)
            DB.readSentimentArray();
        if (isExcludingNegativeSentiment)
            responses = responses.stream().filter(r -> DB.computeSentiment(r) >= 0)
                    .collect(Collectors.toCollection(ArrayList::new));
        else if (isMatchingSentiment)
            responses = responses.stream().filter(r -> compareSentiment(DB.computeSentiment(r),
                    DB.computeSentiment(input))).collect(Collectors.toCollection(ArrayList::new));

        return responses.size() > 0 ? responses : new ArrayList<>(Collections.singletonList("I don't know"));
    }

    /***************************************************************************************************
     *
     * @param responses
     * @param input
     * @return
     */
    private ArrayList<String> rankResponses(ArrayList<String> responses, String input) {

        ArrayList<String> rankedResponses = responses;

        if (isExcludingNegativeSentiment || isMatchingSentiment)
            rankedResponses = rankResponsesOnSentiment(rankedResponses, input);

        return rankedResponses;
    }

    /** ************************************************************************************************
     */
    protected void prepareLine(String line) {

        if (!isNullOrEmpty(line)) {
            int newLineIndex = lines.size();
            lines.add(line);
            //System.out.println(line);
            processDoc(line, newLineIndex);
        }
    }

    /***************************************************************************************************
     *
     * @param responses
     * @return
     */
    private String chooseBestResponse(ArrayList<String> responses) {

        // TODO: Choose best response based on some combination of rankings
        return responses.get(0);
    }

    /** *************************************************************************************************
     * This method takes the best result matched by the ChatBot from the method matchBestInput() as input
     * and filters any profane word(s) found in the result before responding to a query.
     */
    private ArrayList<String> profanityFilter(ArrayList<String> result) {

        ArrayList<String> filteredResult = new ArrayList<>();
        List<String> profanityList = new ArrayList<>();
        String line;
        Properties prop = new Properties();

        try {
            String profanityFile = "src/main/java/chatbot/resourcefiles/profanity-list.txt";
            String str = String.join(",", result);
            BufferedReader br = new BufferedReader(new FileReader(profanityFile));

            while ((line = br.readLine()) != null) {
                profanityList.add(line);
            }
            for (String profaneWord: profanityList) {
                // in the replaceAll() method call, the regEx searches for any spaces before and after the profane word
                // along with the punctuation marks. (?i) nullifies any case sensitive string matching.
                str  = str.replaceAll("[^\\\\s\\\\w( )]*(?i)"+profaneWord+"[[^a-zA-Z0-9\\s][ ][^a-zA-Z0-9\\s]]", " <censored> ");
            }
            filteredResult = new ArrayList<>(Arrays.asList(str.split(",")));
            return filteredResult;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return filteredResult;
    }

    /** *************************************************************************************************
     * @return a list of matches ranked by relevance to the input.
     */
    public TreeMap<Float,ArrayList<Integer>> matchInputFull(String input) {

        //System.out.println("Info in TFIDF.matchInputFull(): input: " + input);
        //System.out.println("Info in TFIDF.matchInputFull(): lines: " + lines);
        ArrayList<String> result = new ArrayList<String>();
        if (isNullOrEmpty(input))
            System.exit(0);
        Integer negone = new Integer(-1);
        processDoc(input,negone);
        calcIDF(lines.size()+1);
        calcOneTFIDF(negone);
        calcDocSim();
        TreeMap<Float,ArrayList<Integer>> sortedSim = new TreeMap<Float,ArrayList<Integer>>();
        if (docSim == null)
            return sortedSim;
        Iterator<Integer> it = docSim.keySet().iterator();
        while (it.hasNext()) {
            Integer i = it.next();
            Float f = docSim.get(i);
            if (sortedSim.containsKey(f)) {
                ArrayList<Integer> vals = sortedSim.get(f);
                vals.add(i);
            }
            else {
                ArrayList<Integer> vals = new ArrayList<Integer>();
                vals.add(i);
                sortedSim.put(f,vals);
            }
        }
        return sortedSim;
    }

    /*****************************************************************
     * Read a file from @param fname and store it in the
     * ArrayList<String> lines member variable.
     * @return an int number of lines
     */
    private void readFile(String fname) {

        System.out.println("Chatbot.readFile() " + fname);
        String line = "";
        BufferedReader omcs = null;
        try {
            String filename = fname;
            if (asResource) {
                URL fileURL = Resources.getResource(fname);
                filename = fileURL.getPath();
            }
            omcs = new BufferedReader(new FileReader(filename));
            /* readLine is a bit quirky :
             * it returns the content of a line MINUS the newline.
             * it returns null only for the END of the stream.
             * it returns an empty String if two newlines appear in a row. */
            ProgressPrinter pp = new ProgressPrinter(1000);
            while ((line = omcs.readLine()) != null) {
                pp.tick();
                prepareLine(line);
            }
            System.out.println();
            omcs.close();
        }
        catch (Exception ex)  {
            System.out.println("Error in readFile(): " + ex.getMessage());
            System.out.println("Error in at line: " + line);
            ex.printStackTrace();
        }
        //System.out.println("Movie lines:\n" + lines);
        //System.out.println("TF:\n" + tf);

        System.out.println();
        calcDFs();
    }

    /** **************************************************************************************************
     * Run with a given file
     */
    private static void run(String fname) throws IOException {

        List<String> documents = null;

        try {
            if (asResource)
                documents = TextFileUtil.readLines(fname, false);
            //documents = TextFileUtil.readFile(fname, false);
        }
        catch (IOException e) {
            System.out.println("Couldn't read document: " + fname + ". Exiting");
            return;
        }
        Chatbot cb;
        ResourceBundle resourceBundle = ResourceBundle.getBundle("corpora");
        if (asResource)
            cb = new Chatbot(documents, resourceBundle.getString("stopWordsDirectoryName"));
        else {
            cb = new Chatbot(resourceBundle.getString("stopWordsDirectoryName"));
            cb.readFile(fname);
        }

        System.out.println("Hi, I'm Cloudio, tell/ask me something. Type 'quit' to exit");

        if (isDevelopment) {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("User: ");
                String input = scanner.nextLine();
                if (input.toLowerCase().trim().equals("quit")) break;
                System.out.print("Cloudio: ");
                System.out.println(cb.matchBestInput(input));
            }
        }
        else {
            while (true) {
                Console c = System.console();
                if (c == null) {
                    System.err.println("No console.");
                    System.exit(1);
                }
                String input = c.readLine("> ");
                if (input.toLowerCase().trim().equals("quit")) System.exit(1);
                System.out.println("Cloudio:" + cb.matchBestInput(input));
            }
        }
    }

    /*************************************************************************************************
     *
     * @param args
     */
    public static void main(String[] args) {

        try {
            if (args != null && args.length > 0 && args[0].equals("-h")) {
                System.out.println("Usage: ");
                System.out.println("TFIDF -h         % show this help info");
                System.out.println("      -f fname   % run program using a particular input file");
                System.out.println("      -d fname   % development mode using a particular input file");
                System.out.println("      -d -s      % development mode using s3 to load input files");
                System.out.println("adding -snn      % filters responses by non-negative sentiment");
                System.out.println("adding -sm       % filters responses by matching sentiment");
            }
            else if (args != null && args.length > 1 && args[0].equals("-f")) {
                asResource = false;
                isDevelopment = false;
                if (ArrayUtils.contains(args, "-snn")) isExcludingNegativeSentiment = true;
                if (ArrayUtils.contains(args, "-sm")) isMatchingSentiment = true;
                run(args[1]);
            }
            else if (args != null && args.length > 1 && args[0].equals("-d")) {
                asResource = false;
                isDevelopment = true;

                if (ArrayUtils.contains(args, "-snn")) isExcludingNegativeSentiment = true;
                if (ArrayUtils.contains(args, "-sm")) isMatchingSentiment = true;
                if (args[1].equals("-s")) {
                    String newFileName = "/home/vish/Documents/chatbot/cornell_movie_dialgos_corpus_parsed/" +
                            "movie_lines_parsed.txt";
                    run(newFileName);
                } else {
                    run(args[1]);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}