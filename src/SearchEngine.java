import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class SearchEngine {
    /////////////////////////////////////////   basic   //////////////////////////////////////
    private final String folderPath;
    private final int version;

    /////////////////////////////////////////   phase 1   //////////////////////////////////////
    private String[] filesContent;      // each element is content of a file
    private final int filesStartNum = 1;    // number of the first file in the folder
    private int filesNumber;            // number of files in the folder
    private ArrayList[] directIndex;    // each element is an array of all words in specific document
    // each element includes the word, the word file number, the word index
    private ArrayList<Pair<String, Pair<Integer, Integer>>> words;
    // each element is a word and the words posting list
    private ArrayList<Pair<String, PostingList>> invertedIndexNew;
    private final int freqWordLimit = 500;    // minimum frequency for a word to be frequent word then removed
    // stop words of the the collection collected from frequent words in inverted index creation and
    // from idf score in tf-idf score calculation
    private ArrayList<String> stopWords;

    /////////////////////////////////////////   phase 2  //////////////////////////////////////
    private final double idxElim = 0.5; // fraction of query words should be in a document to be considered
    private final int kNum = 5; // number of results returns
    private final int champNum = 3; // size of champion list for each word

    /////////////////////////////////////////   phase 3  //////////////////////////////////////
    private final int clusterNumber;
    private final int[] documentCluster;
    private ArrayList[] clusterWordsWeights;

    SearchEngine(String folderPath, int version) {
        ////    basic
        this.folderPath = folderPath;       // path of the files folder
        this.version = version;

        ////    phase 1
        filesNumber = get_file_number();
        filesContent = new String[filesNumber];
        directIndex = new ArrayList[filesNumber];
        words = new ArrayList<>();
        invertedIndexNew = new ArrayList<>();
        stopWords = new ArrayList<>();

        //// phase 3
        clusterNumber = get_cluster_number();
        documentCluster = new int[filesNumber];
        clusterWordsWeights = new ArrayList[clusterNumber];
    }

    /////////////////////////////////// initialization functions  ////////////////////////////

    private int get_file_number() {
        if (version == 1 || version == 2) {
            return Objects.requireNonNull(new File(folderPath).listFiles()).length;
        }
        if (version == 3) {
            int file_num = 0;
            File folder = new File(folderPath);
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                File[] files2 = file.listFiles();
                assert files2 != null;
                file_num += files2.length;
            }
            return file_num;
        }
        return -1;
    }

    private int get_cluster_number() {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        assert files != null;
        return files.length;
    }

    /////////////////////////////////// configuration functions  ////////////////////////////

    public void configureSearchEngine1() {
        System.out.println("configuration...");
        readFiles();    // read each file and put content of that in one element of @filesContent
        tokenize();     // tokenize each file content and put tokens of each file in one element of @directIndex
        createInvertedIndex();  // creat inverted index and put each word in one element of @invertedIndexNew
//        System.out.println("sd1");
    }

    public void configureSearchEngine2() {
        System.out.println("configuration...");
        readFiles();    // read each file and put content of that in one element of @filesContent
        tokenize();     // tokenize each file content and put tokens of each file in one element of @directIndex
        createInvertedIndex();  // creat inverted index and put each word in one element of @invertedIndexNew
        tf_idfWeightSet();    // set tf-idf scores of each word in each document
//        System.out.println("sd2");
    }

    public void configureSearchEngine3() {
        System.out.println("configuration...");
        readFiles2();   // read each file and put content of that in one element of @filesContent
        tokenize();     // tokenize each file content and put tokens of each file in one element of @directIndex
        createInvertedIndex();  // creat inverted index and put each word in one element of @invertedIndexNew
        tf_idfWeightSet();    // set tf-idf scores of each word in each document
        setClusterCenter();   // set center of each cluster in one element of @clusterWordsWeights
//        System.out.println("sd");
    }

    /////////////////////////////////// configuration 1

    private void readFiles() {
        // get files pointer and sort them in correct numeral order
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        assert files != null;
        Arrays.sort(files, (f1, f2) -> {
            try {
                int i1 = Integer.parseInt(f1.getName().substring(0, f1.getName().length() - 4));
                int i2 = Integer.parseInt(f2.getName().substring(0, f2.getName().length() - 4));
                return i1 - i2;
            } catch (NumberFormatException e) {
                throw new AssertionError(e);
            }
        });

        // get content of files
        for (int i = 0; i < filesNumber; i++) {
            try {
                Scanner reader = new Scanner(files[i]);
                filesContent[i] = "";
                while (reader.hasNextLine()) {
                    String data = reader.nextLine();
                    filesContent[i] += " " + data;
                }
                reader.close();
            } catch (FileNotFoundException e) {
                System.out.println("file didn't found");
                e.printStackTrace();
            }
        }
    }

    private void tokenize() {
        for (int i = 0; i < filesNumber; i++) {
            // get list of words
            String[] parts = filesContent[i].split(" ");
            ArrayList<String> tokens = new ArrayList<>();
            Collections.addAll(tokens, parts);

            for (int j = 0; j < tokens.size(); j++) {
                // remove everything except persian alphabets
                tokens.set(j, tokens.get(j).replaceAll(
                        "[^\u0621\u0623\u0626\u0624\u0622\u0627\u0628\u067E" +
                                "\u062A\u062B\u062C\u0686\u062D\u062E\u062F\u0630" +
                                "\u0631\u0632\u0698\u0633\u0634\u0635\u0636\u0637" +
                                "\u0638\u0639\u063A\u0641\u0642\u06A9\u06AF\u0644" +
                                "\u0645\u0646\u0648\u0647\u06CC]", ""));

                String newToken = normalization(tokens.get(j));
                tokens.set(j, newToken);


                // remove empty strings
                if (tokens.get(j).equals("")) {
                    tokens.remove(j);
                    j--;
                }

            }
            directIndex[i] = tokens;
        }
    }

    private String normalization(String token) {
        if (token.endsWith("های")) {
            return token.replace("های", "");
        }
        if (token.endsWith("هایی")) {
            return token.replace("هایی", "");
        }
        if (token.endsWith("هایش")) {
            return token.replace("هایش", "");
        }
        if (token.endsWith("تر")) {
            if (!(token.startsWith("به") || token.startsWith("بد")))
                return token.replace("تر", "");
        }
        if (token.endsWith("ترین")) {
            return token.replace("ترین", "");
        }
        if (token.endsWith("گان")) {
            if (!token.startsWith("گر")) {
                return token.replace("گان", "ه");
            }
        }
        if (token.endsWith("ها")) {
            return token.replace("ها", "");
        }
        if (token.endsWith("اش")) {
            return token.replace("اش", "");
        }
        if (token.endsWith("ی")) {
            return token.replace("ی", "");
        }
        if (token.endsWith("یی")) {
            return token.replace("یی", "");
        }
        if (token.startsWith("می")) {
            if (token.endsWith("م") || token.endsWith("ی") || token.endsWith("د")) {
                return token.replace("می", "");
            }
        }
        return token;
    }

    private void createInvertedIndex() {
        createWordsList();    // create list of words and each word is an element of @words
        sortWords();    // sort all words of @words array in alphabetic order
        // create inverted index from sorted words and each word is one element of @invertedIndexNew
        invertedIndex();
    }

    private void createWordsList() {
        for (int i = 0; i < directIndex.length; i++) {
            for (int j = 0; j < directIndex[i].size(); j++) {
                words.add(new Pair<>((String) directIndex[i].get(j), new Pair<>(i + filesStartNum, j)));
            }
        }
    }

    private void sortWords() {
        words.sort((o1, o2) -> {
            String str1 = o1.getKey();
            String str2 = o2.getKey();
            int l1 = str1.length();
            int l2 = str2.length();
            int lMin = Math.min(l1, l2);

            for (int i = 0; i < lMin; i++) {
                int str1_ch = getCharValue(str1.substring(i, i + 1));
                int str2_ch = getCharValue(str2.substring(i, i + 1));

                if (str1_ch != str2_ch) {
                    return str1_ch - str2_ch;
                }
            }

            if (l1 != l2) {
                return l1 - l2;
            } else {
                return 0;
            }
        });
    }

    private int getCharValue(String alph) {
        switch (alph) {
            case "ء":
                return 0;
            case "آ":
                return 1;
            case "ا":
                return 2;
            case "أ":
                return 3;
            case "ب":
                return 4;
            case "پ":
                return 5;
            case "ت":
                return 6;
            case "ث":
                return 7;
            case "ج":
                return 8;
            case "چ":
                return 9;
            case "ح":
                return 10;
            case "خ":
                return 11;
            case "د":
                return 12;
            case "ذ":
                return 13;
            case "ر":
                return 14;
            case "ز":
                return 15;
            case "ژ":
                return 16;
            case "س":
                return 17;
            case "ش":
                return 18;
            case "ص":
                return 19;
            case "ض":
                return 20;
            case "ط":
                return 21;
            case "ظ":
                return 22;
            case "ع":
                return 23;
            case "غ":
                return 24;
            case "ف":
                return 25;
            case "ق":
                return 26;
            case "ک":
                return 27;
            case "گ":
                return 28;
            case "ل":
                return 29;
            case "م":
                return 30;
            case "ن":
                return 31;
            case "و":
                return 32;
            case "ؤ":
                return 33;
            case "ه":
                return 34;
            case "ی":
                return 35;
            case "ئ":
                return 36;
            default:
//                System.out.println("errr : " + alph);
                return -1;
        }
    }

    private void invertedIndex() {
        ArrayList<Integer> freqWordsIndex = new ArrayList<>();
        for (Pair<String, Pair<Integer, Integer>> stringPairPair : words) {
            String word = stringPairPair.getKey();
            int docID = stringPairPair.getValue().getKey();
            int position = stringPairPair.getValue().getValue();
//            System.out.println(word + " " + docID + " " + position);

            // start of list
            if (invertedIndexNew.isEmpty()) {
                invertedIndexNew.add(new Pair<>(word, new PostingList(filesNumber, champNum)));
                invertedIndexNew.get(0).getValue().newDocID(docID, position);
            } else {
//                System.out.println(lastInvertedIndex);
                // repeated word
                if (word.equals(invertedIndexNew.get(invertedIndexNew.size() - 1).getKey())) {
                    if (invertedIndexNew.get(invertedIndexNew.size() - 1).getValue().freq == freqWordLimit) {
                        freqWordsIndex.add(invertedIndexNew.size() - 1);
                    }
                    // repeated docID
                    if (docID == invertedIndexNew.get(invertedIndexNew.size() - 1).getValue().lastDocID()) {
                        invertedIndexNew.get(invertedIndexNew.size() - 1).getValue().newPosition(position);
                        // new docID
                    } else {
                        invertedIndexNew.get(invertedIndexNew.size() - 1).getValue().newDocID(docID, position);
                    }
                    // new word
                } else {
                    invertedIndexNew.add(new Pair<>(word, new PostingList(filesNumber, champNum)));
                    invertedIndexNew.get(invertedIndexNew.size() - 1).getValue().newDocID(docID, position);
                }
            }
        }
        removeFrequentWords(freqWordsIndex);
    }

    private void removeFrequentWords(ArrayList<Integer> freqWordsIndex) {
        for (int i = freqWordsIndex.size() - 1; i >= 0; i--) {
//            System.out.println(invertedIndexNew.get(freqWordsIndex.get(i)).getKey());
            stopWords.add(invertedIndexNew.get(freqWordsIndex.get(i)).getKey());
            invertedIndexNew.remove((int) freqWordsIndex.get(i));
        }
    }

    /////////////////////////////////// configuration 2

    private void tf_idfWeightSet() {
        ArrayList<Integer> eliminatedIndex = new ArrayList<>();
        for (int i = 0; i < invertedIndexNew.size(); i++) {
            if (invertedIndexNew.get(i).getValue().setTf_idfScores()) {
                eliminatedIndex.add(i);
            }
        }
        for (int i = eliminatedIndex.size() - 1; i >= 0; i--) {
//            System.out.println(invertedIndexNew.get(eliminatedIndex.get(i)).getKey());
            stopWords.add(invertedIndexNew.get(eliminatedIndex.get(i)).getKey());
            invertedIndexNew.remove((int) eliminatedIndex.get(i));
        }
    }

    /////////////////////////////////// configuration 3

    private void readFiles2() {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        int file_num_arr[] = new int[files.length];
        assert files != null;
        int cnt = 0;
        for (int i = 0; i < files.length; i++) {
            File[] files2 = files[i].listFiles();
            assert files2 != null;
            file_num_arr[i] = files2.length;
            Arrays.sort(files2, (f1, f2) -> {
                try {
                    int i1 = Integer.parseInt(f1.getName().substring(0, f1.getName().length() - 4));
                    int i2 = Integer.parseInt(f2.getName().substring(0, f2.getName().length() - 4));
                    return i1 - i2;
                } catch (NumberFormatException e) {
                    throw new AssertionError(e);
                }
            });
            for (int j = 0; j < file_num_arr[i]; j++) {
                try {
                    Scanner reader = new Scanner(files2[j]);
                    filesContent[cnt] = "";
                    while (reader.hasNextLine()) {
                        String data = reader.nextLine();
                        filesContent[cnt] += " " + data;
                    }
                    reader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("file didn't found");
                    e.printStackTrace();
                }
                documentCluster[cnt] = i;
                cnt++;
            }
        }
    }

    private void setClusterCenter() {
        int cnt = 0;
        for (int i = 0; i < clusterNumber; i++) {
            // each element of array is one pair, first element of pair is the word and second element of pair is weights
            // of the word in documents which are in this cluster
            ArrayList<Pair<String, ArrayList<Double>>> cluster_words = new ArrayList<>();
            for (; documentCluster[cnt] == i; cnt++) {
                // search for the word
                for (int k = 0; k < directIndex[cnt].size(); k++) {
                    String thisWord = (String) directIndex[cnt].get(k);
                    if (!stopWords.contains(thisWord)) {
                        int l = get_word_index_binary(thisWord);
                        double word_weight = invertedIndexNew.get(l).getValue().tf_idfScores.get(cnt);

                        int m;
                        for (m = 0; m < cluster_words.size(); m++) {
                            if (cluster_words.get(m).getKey().equals(thisWord)) {
                                break;
                            }
                        }
                        if (m == cluster_words.size()) {
                            ArrayList<Double> tmpArr = new ArrayList<>();
                            tmpArr.add(word_weight);
                            cluster_words.add(new Pair<>(thisWord, tmpArr));
                        } else {
                            ArrayList<Double> tmpArr = cluster_words.get(m).getValue();
                            cluster_words.remove(m);
                            tmpArr.add(word_weight);
                            cluster_words.add(new Pair<>(thisWord, tmpArr));
                        }

                    }
                }
                if (cnt == directIndex.length - 1) {
                    break;
                }
            }
//            System.out.println("a;slkd");
            ArrayList<Pair<String, Double>> weights = new ArrayList<>();
            for (int j = 0; j < cluster_words.size(); j++) {
                ArrayList<Double> arr = cluster_words.get(j).getValue();
                double sum = 0.0;
                for (int k = 0; k < arr.size(); k++) {
                    sum += arr.get(k);
                }
                double average = sum / arr.size();
                weights.add(new Pair<>(cluster_words.get(j).getKey(), average));
            }
            clusterWordsWeights[i] = weights;
        }
    }

    private int get_word_index_binary(String word) {
        int l = 0;
        int r = invertedIndexNew.size() - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;

            String inWord = invertedIndexNew.get(m).getKey();
            int res = comparePer(word, inWord);

            if (res == 0)
                return m;

            if (res > 0)
                l = m + 1;

            else
                r = m - 1;
        }
//        System.out.println("eroooooooooor " + word);
        return -1;
    }

    private int comparePer(String str1, String str2) {
        int l1 = str1.length();
        int l2 = str2.length();
        int lMin = Math.min(l1, l2);

        for (int i = 0; i < lMin; i++) {
            int str1_ch = getCharValue(str1.substring(i, i + 1));
            int str2_ch = getCharValue(str2.substring(i, i + 1));

            if (str1_ch != str2_ch) {
                return str1_ch - str2_ch;
            }
        }

        if (l1 != l2) {
            return l1 - l2;
        } else {
            return 0;
        }
    }

    /////////////////////////////////// search1 functions  ////////////////////////////
    public void search1() {
        while (true) {
            String query = getInput();
            String[] queryWords = query.split(" ");
            if (queryWords.length == 1) {
                singleWordSearch(query);
            } else {
                multipleWordSearch(queryWords, true, true);
            }
        }
    }

    private String getInput() {
        System.out.println();
        System.out.println("Enter your search words (use space between them)");
        Scanner scanner = new Scanner(System.in);
        String inp = scanner.nextLine();
        if (inp.equals("end")) {
            System.exit(0);
        }
        return inp;
    }

    private void singleWordSearch(String query) {
        String queryNorm = normalization(query);
        int wordIndex = findWordIndex(queryNorm);
        if (wordIndex == -1) {
            printResult(0, null, null);
        } else {
            printResult(1, invertedIndexNew.get(wordIndex).getValue().getDocIDs(), null);
        }
    }

    private ArrayList<Pair<Integer, Integer>> multipleWordSearch(String[] queryWords, boolean isPrint, boolean norm) {
        ArrayList<Pair<Integer, Integer>> docsNum = new ArrayList<>();
        for (String queryWord : queryWords) {
            String queryWordNorm = queryWord;
            if (norm) {
                queryWordNorm = normalization(queryWord);
            }
            int idx = findWordIndex(queryWordNorm);
            if (idx != -1) {
//                ArrayList<Integer> ls = invertedIndex.get(findWordIndex(queryWord)).getValue();
                ArrayList<Integer> ls = invertedIndexNew.get(findWordIndex(queryWordNorm)).getValue().getDocIDs();
                for (Integer l : ls) {
                    boolean breakHappen = false;
                    for (int k = 0; k < docsNum.size(); k++) {
                        if (docsNum.get(k).getKey().equals(l)) {
                            docsNum.set(k, new Pair<>(docsNum.get(k).getKey(), docsNum.get(k).getValue() + 1));
                            breakHappen = true;
                            break;
                        }
                    }
                    if (!breakHappen) {
                        docsNum.add(new Pair<>(l, 1));
                    }
                }
            }
        }
        sortDocs(docsNum);
        if (isPrint)
            printResult(2, null, docsNum);
        return docsNum;
    }

    private int findWordIndex(String word) {
        for (int i = 0; i < invertedIndexNew.size(); i++) {
            if (word.equals(invertedIndexNew.get(i).getKey())) {
                return i;
            }
        }
        return -1;
    }

    private void sortDocs(ArrayList<Pair<Integer, Integer>> docsNum) {
        docsNum.sort((o1, o2) -> o2.getValue() - o1.getValue());
    }

    private void printResult(int mode, ArrayList<Integer> docIDs, ArrayList<Pair<Integer, Integer>> docsNum) {
        switch (mode) {
            case 0:
                System.out.println("The word didn't found in documents");
                break;
            case 1:
                System.out.println("The word found in these documents : ");
                for (Integer docID : docIDs) {
                    System.out.println(docID);
                }
                break;
            case 2:
                System.out.println("These words found in these documents : ");
                System.out.println("DocID  freq");
                for (Pair<Integer, Integer> aDocsNum : docsNum) {
                    System.out.println("  " + aDocsNum.getKey() + "     " + aDocsNum.getValue());
                }
                break;
        }
    }

    /////////////////////////////////// search2 functions  ////////////////////////////
    public void search2() {
        while (true) {
            String query = getInput();
            String[] queryWords = query.split(" ");
            tf_idfSearch(queryWords);
        }
    }

    private void tf_idfSearch(String[] words) {
        // remove stop words from query and returns remaining words in @newWords
        ArrayList<String> newWords = removeExtraWords(words);
        // return an array which each element of the array is a pair of word and its corresponding frequency
        ArrayList<Pair<String, Integer>> arr = getQueryWeight(newWords);
        // normalize term frequency of words with formula
        ArrayList<Pair<String, Double>> newArr = normTf(arr);

        // get index of documents which cosine score should be calculated for them
        ArrayList<Integer> indexEliminationDocs = findEliminatedIndex(newArr, newWords);
        // calculate cosine score between documents and query
        ArrayList<Pair<Integer, Double>> cosArr = calcDocsCosine(newArr, indexEliminationDocs);
        // create max heap with cosine value of documents
        MaxHeap maxHeap = createHeap(cosArr);
        // extract k documents with maximum cosine score from the heap
        getKMax(maxHeap, cosArr);

    }

    private ArrayList<String> removeExtraWords(String[] words) {
        ArrayList<String> newWords = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            if (!stopWords.contains(words[i])) {
                newWords.add(normalization(words[i]));
            }
        }
        return newWords;
    }

    private ArrayList<Pair<String, Integer>> getQueryWeight(ArrayList<String> newWords) {
        ArrayList<Pair<String, Integer>> arr = new ArrayList<>();
        for (int i = 0; i < newWords.size(); i++) {
            if (arr.size() == 0) {
                arr.add(new Pair<>(newWords.get(i), 1));
            } else {
                boolean breakHappen = false;
                for (int j = 0; j < arr.size(); j++) {
                    if (arr.get(j).getKey().equals(newWords.get(i))) {
                        arr.set(j, new Pair<>(newWords.get(i), arr.get(j).getValue() + 1));
                        breakHappen = true;
                        break;
                    }
                }
                if (!breakHappen) {
                    arr.add(new Pair<>(newWords.get(i), 1));
                }
            }
        }
        return arr;
    }

    private ArrayList<Pair<String, Double>> normTf(ArrayList<Pair<String, Integer>> arr) {
        ArrayList<Pair<String, Double>> newArr = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            newArr.add(new Pair<>(arr.get(i).getKey(), (1.0 + Math.log10(arr.get(i).getValue()))));
        }
        return newArr;
    }

    private ArrayList<Integer> findEliminatedIndex(ArrayList<Pair<String, Double>> queryArr, ArrayList<String> newWords) {
        String[] newWords2 = new String[newWords.size()];
        for (int i = 0; i < newWords.size(); i++) {
            newWords2[i] = newWords.get(i);
        }

        ArrayList<Integer> championLists = findChampionList(queryArr);

        ArrayList<Pair<Integer, Integer>> docsNum = multipleWordSearch(newWords2, false, false);
        ArrayList<Integer> indexEliminationDocs = new ArrayList<>();
        for (int i = 0; i < docsNum.size(); i++) {
            if (championLists.contains(docsNum.get(i).getKey())) {                              // champ
                if (((docsNum.get(i).getValue() + 0.0) / queryArr.size()) > idxElim) {          // invert
                    indexEliminationDocs.add(docsNum.get(i).getKey() - filesStartNum);
                }
            }
        }
        return indexEliminationDocs;
    }

    private ArrayList<Integer> findChampionList(ArrayList<Pair<String, Double>> queryArr) {
        ArrayList<Integer> championLists = new ArrayList<>();
        for (int i = 0; i < queryArr.size(); i++) {
            int idx = findWordIndex(queryArr.get(i).getKey());
            if (idx != -1) {
                for (int j = 0; j < invertedIndexNew.get(idx).getValue().championList.size(); j++) {
                    int tmp = invertedIndexNew.get(idx).getValue().championList.get(j);
                    if (!championLists.contains(tmp)) {
                        championLists.add(tmp);
                    }
                }
            }
        }
        return championLists;
    }

    private ArrayList<Pair<Integer, Double>> calcDocsCosine(ArrayList<Pair<String, Double>> queryArr,
                                                            ArrayList<Integer> indexEliminationDocs) {
        ArrayList<ArrayList<Double>> tfs = new ArrayList<>();
        for (int i = queryArr.size() - 1; i >= 0; i--) {
            boolean breakHappen = false;
            for (int j = 0; j < invertedIndexNew.size(); j++) {
                if (queryArr.get(i).getKey().equals(invertedIndexNew.get(j).getKey())) {
                    tfs.add(invertedIndexNew.get(j).getValue().tf_idfScores);
                    breakHappen = true;
                    break;
                }
            }
            if (!breakHappen) {
                queryArr.remove(i);
            }
        }

        ArrayList<Double> numerators = new ArrayList<>();
        for (int i : indexEliminationDocs) {
            double sum = 0.0;
//            System.out.println(tfs.size());
            for (int j = 0; j < queryArr.size(); j++) {
                sum += queryArr.get(j).getValue() * tfs.get(j).get(i);
            }
            numerators.add(sum);
        }

        ArrayList<Double> denominator = new ArrayList<>();
//        double d1 = 0.0;
//        for (int i = 0; i < queryArr.size(); i++) {
//            d1 += Math.pow(queryArr.get(i).getValue(), 2);
//        }
//        d1 = Math.sqrt(d1);

        double d2 = 0.0;
        for (int i : indexEliminationDocs) {
            double sum = 0.0;
            for (int j = 0; j < invertedIndexNew.size(); j++) {
                sum += Math.pow(invertedIndexNew.get(j).getValue().tf_idfScores.get(i), 2);
            }
            sum = Math.sqrt(sum);
            denominator.add(sum);
        }

        ArrayList<Pair<Integer, Double>> cosine = new ArrayList<>();
        for (int i = 0; i < numerators.size(); i++) {
            cosine.add(new Pair<>(indexEliminationDocs.get(i) + filesStartNum, numerators.get(i) / denominator.get(i)));
        }
        return cosine;
    }

    private MaxHeap createHeap(ArrayList<Pair<Integer, Double>> cosArr) {
        MaxHeap maxHeap = new MaxHeap(filesNumber);
        for (int i = 0; i < cosArr.size(); i++) {
            maxHeap.insert(cosArr.get(i));
        }
        return maxHeap;
    }

    private void getKMax(MaxHeap maxHeap, ArrayList<Pair<Integer, Double>> cosArr) {
        for (int i = 0; i < Math.min(cosArr.size(), kNum); i++) {
            Pair<Integer, Double> tmp = maxHeap.extractMax();
            System.out.println("The max val is " + tmp.getValue() + " for doc " + tmp.getKey());
        }
    }

    /////////////////////////////////// search3 functions  ////////////////////////////

    public void search3() {
        while (true) {
            String query = getInput();
            String[] queryWords = query.split(" ");
            tf_idf_SearchCluster(queryWords);
        }
    }

    private void tf_idf_SearchCluster(String[] words) {
        // remove stop words from query and returns remaining words in @newWords
        ArrayList<String> newWords = removeExtraWords(words);
        // return an array which each element of the array is a pair of word and its corresponding frequency
        ArrayList<Pair<String, Integer>> arr = getQueryWeight(newWords);
        // normalize term frequency of words with formula
        ArrayList<Pair<String, Double>> newArr = normTf(arr);

        // first part
        // calculate cosine score between cluster centers and query
        ArrayList<Pair<Integer, Double>> cosArr2 = calcDocsCosine2(newArr);
        // find maximum similarity between clusters then insert index of the cluster in @clusterIndex
        int clusterIndex = getClusterIndex(cosArr2);
//            System.out.println(clusterIndex);

        // second part
        ArrayList<Integer> indexEliminationDocs = findEliminatedIndex2(clusterIndex);
        ArrayList<Pair<Integer, Double>> cosArr = calcDocsCosine(newArr, indexEliminationDocs);
        MaxHeap maxHeap = createHeap(cosArr);
        getKMax(maxHeap, cosArr);
    }

    private ArrayList<Pair<Integer, Double>> calcDocsCosine2(ArrayList<Pair<String, Double>> queryArr) {
        ArrayList<ArrayList<Double>> tfs = new ArrayList<>();
        for (int i = queryArr.size() - 1; i >= 0; i--) {
            boolean breakHappen = false;
            for (int j = 0; j < invertedIndexNew.size(); j++) {
                if (queryArr.get(i).getKey().equals(invertedIndexNew.get(j).getKey())) {
//                    tfs.add(invertedIndexNew.get(j).getValue().tf_idfScores);
                    breakHappen = true;
                    break;
                }
            }
            if (!breakHappen) {
                queryArr.remove(i);
            }
        }


        ArrayList<Double> numerators = new ArrayList<>();
        for (ArrayList<Pair> arr : clusterWordsWeights) {
            double sum = 0.0;
//            System.out.println(tfs.size());
            for (int j = 0; j < queryArr.size(); j++) {
//                sum += queryArr.get(j).getValue() * tfs.get(j).get(i);
                for (int k = 0; k < arr.size(); k++) {
                    if (arr.get(k).getKey().equals(queryArr.get(j).getKey())) {
                        sum += queryArr.get(j).getValue() * (double) arr.get(k).getValue();
                    }
                }
            }
            numerators.add(sum);
        }

        ArrayList<Double> denominator = new ArrayList<>();
//        double d1 = 0.0;
//        for (int i = 0; i < queryArr.size(); i++) {
//            d1 += Math.pow(queryArr.get(i).getValue(), 2);
//        }
//        d1 = Math.sqrt(d1);

        double d2 = 0.0;
        for (ArrayList<Pair> arr : clusterWordsWeights) {
            double sum = 0.0;
//                sum += Math.pow(invertedIndexNew.get(j).getValue().tf_idfScores.get(i), 2);
            for (int k = 0; k < arr.size(); k++) {
                sum += Math.pow((double) arr.get(k).getValue(), 2);
            }

            sum = Math.sqrt(sum);
            denominator.add(sum);
        }

        ArrayList<Pair<Integer, Double>> cosine = new ArrayList<>();
        for (int i = 0; i < clusterNumber; i++) {
            cosine.add(new Pair<>(i, numerators.get(i) / denominator.get(i)));
        }
//        System.out.println("sdfsafsdfgt");
        return cosine;
    }

    private int getClusterIndex(ArrayList<Pair<Integer, Double>> cosArr) {
        double max_val = -111111.0;
        int max_val_index = -1;
        for (int i = 0; i < cosArr.size(); i++) {
            if (max_val < cosArr.get(i).getValue()) {
                max_val = cosArr.get(i).getValue();
                max_val_index = i;
            }
        }
        return max_val_index;
    }

    private ArrayList<Integer> findEliminatedIndex2(int clusterIndex) {
        ArrayList<Integer> indexEliminationDocs = new ArrayList<>();
        for (int i = 0; i < documentCluster.length; i++) {
            if (documentCluster[i] == clusterIndex) {
                indexEliminationDocs.add(i);
            }
        }
        return indexEliminationDocs;
    }
}