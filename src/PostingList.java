import javafx.util.Pair;

import java.util.ArrayList;

public class PostingList {
    //////////////////////////////////////      phase 1
    public int freq;    // frequency of the word in collection
    // each elements of the array includes two part
    // first part is the first number that is the docID
    // second part is other numbers that are indexes of the word in that docID
    public ArrayList<ArrayList<Integer>> postingList;

    //////////////////////////////////////      phase 2
    public ArrayList<Double> tf_idfScores;
    public ArrayList<Integer> championList;
    private ArrayList<Pair<Integer, Integer>> chPair;
    // if idf of a word is less than idfThreshold the word will be removed
    private final double idfThreshold = 0.3;
    private int filesNumber;
    private final int filesStartNum = 1;
    private final int champNum;

    PostingList(int fn, int chNum) {
        // phase 1
        freq = 0;
        postingList = new ArrayList<>();

        // phase 2
        tf_idfScores = new ArrayList<>();
        championList = new ArrayList<>();
        chPair = new ArrayList<>();
        filesNumber = fn;
        champNum = chNum;
    }

    ////////        phase 1  functions          ///////////

    ////    configuration
    private int lastIndex() {
        return postingList.size() - 1;
    }

    public void newDocID(int docID, int position) {
        postingList.add(new ArrayList<>());
        postingList.get(lastIndex()).add(docID);
        postingList.get(lastIndex()).add(position);
        freq++;
    }

    public void newPosition(int position) {
        postingList.get(lastIndex()).add(position);
        freq++;
    }

    public int lastDocID() {
        return postingList.get(lastIndex()).get(0);
    }

    ////    search
    public ArrayList<Integer> getDocIDs() {
        ArrayList<Integer> arr = new ArrayList<>();
        for (ArrayList<Integer> integers : postingList) {
            arr.add(integers.get(0));
        }
        return arr;
    }

    ////////        phase 2  functions          ///////////

    ////    configuration
    public boolean setTf_idfScores() {
        int nt = postingList.size();
        double idf = Math.log10((filesNumber + 0.0) / nt);
        if (idf < idfThreshold) {
            return true;
        }
        for (int i = filesStartNum; i < filesNumber + filesStartNum; i++) {
            int tf = calcTfScore(i);
            if (tf == 0) {
                tf_idfScores.add(0.0);
            } else {
                chPair.add(new Pair<>(i, tf));
                tf_idfScores.add(idf * (1.0 + Math.log10(tf)));
            }
//            tf_idfScores.add(idf * tf);
        }
        setChampionList();
        return false;
    }

    private int calcTfScore(int docNum) {
        int tf = 0;
        for (ArrayList<Integer> integers : postingList) {
            if (docNum == integers.get(0)) {
                tf = integers.size() - 1;
                break;
            }
        }
        //        return 1.0 + Math.log10(tf);
        return tf;
    }

    private void setChampionList() {
        chPair.sort((o1, o2) -> o2.getValue() - o1.getValue());
        for (int i = 0; i < Math.min(champNum, chPair.size()); i++) {
            championList.add(chPair.get(i).getKey());
        }
    }
}
