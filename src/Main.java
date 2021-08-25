public class Main {

    static int version = 3;

    // path of the files folder
    private static final String folderPath = "C:\\Users\\pc\\Desktop\\darsi\\bazyabi\\3\\dataSet";

    public static void main(String[] args) {

        SearchEngine se = new SearchEngine(folderPath, version);
        if (version == 1) {
            se.configureSearchEngine1();
            se.search1();

        }
        if (version == 2) {
            se.configureSearchEngine2();
            se.search2();
        }
        if (version == 3){
            se.configureSearchEngine3();
            se.search3();

        }

    }
}
