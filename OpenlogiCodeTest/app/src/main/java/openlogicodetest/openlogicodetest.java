package openlogicodetest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class openlogicodetest {

    /** 最大探索回数(変更する場合はこの値を変えて下さい) */
    private static final int MAX_SEARCH = 20;

    private static final AtomicInteger searchCount = new AtomicInteger(0);

    /** 訪問済みセット */
    private static Set<String> visited = new HashSet<>();

    // 親子関係を保持するMap(親・子のリスト)
    private static Map<String, List<String>> treeMap = new LinkedHashMap<>();

    private static Map<String, Integer> treeDepthMap = new LinkedHashMap<>();

    /** ログ出力 */
    private static final Logger logger = Logger.getLogger(openlogicodetest.class.getName());

    /** $ */
    private static final String SUFFIX_$ = "$";

    /** @ */
    private static final String SUFFIX_AT = "@";

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        /** 開始URL */
        String START_URL = "https://ja.wikipedia.org/wiki/%E3%82%A8%E3%83%AA%E3%82%A6%E3%83%89%E3%83%BB%E3%82%AD%E3%83%97%E3%83%81%E3%83%A7%E3%82%B2";

        /** 開始キーワード */
        String START_KEY_WORD = "エリウド・キプチョゲ";

        // 幅優先探索として探索する
        searchAsBfs(START_URL, START_KEY_WORD, 0);

        // 探索したものを出力する
        printTree(START_KEY_WORD, 0, new HashSet<>());

        System.out.println("\nExecution Time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * 幅優先探索します
     * 
     * @param startUrl     スタートするURL
     * @param startKeyword スタートするキーワード
     * @throws InterruptedException
     */
    public static void searchAsBfs(String startUrl, String startKeyword, int depth) {

        // URL,キーワードをキューとして保持
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[] { startUrl, startKeyword, depth });

        // 一度訪問したキーワードを保持
        visited.add(startKeyword);
        treeDepthMap.put(startKeyword, depth);

        // 探索ロジック
        // 幅優先探索で実装する(要件No.4)
        // 最大20回までの探索(要件No.2)
        while (!queue.isEmpty() && searchCount.get() < MAX_SEARCH) {
            Object[] current = queue.poll();
            String url = (String) current[0];
            String keyword = (String) current[1];
            int currentDepth = (int) current[2];
            searchCount.incrementAndGet();

            List<String> children = new ArrayList<>();
            try {
                // リクエスト間を1秒以上空けることでサーバへの負荷を抑える(要件No.6)
                Thread.sleep(1000);
                Document doc = Jsoup.connect(url).get();
                Element firstP = doc.select(".mw-parser-output > p").first();
                if (firstP != null) {
                    Elements links = firstP.select("a[href^=/wiki/]");
                    for (Element link : links) {
                        String linkText = link.text();
                        String href = link.attr("href");
                        if (linkText.isEmpty() || href.substring(6).contains(":")) {
                            continue;
                        }

                        int nextDepth = currentDepth + 1;
                        if (!treeDepthMap.containsKey(linkText)) {
                            treeDepthMap.put(linkText, nextDepth);
                        }

                        // キーワード重複時、@を付けてリストに追加し、Queueには入れない
                        if (visited.contains(linkText)) {
                            children.add(linkText);
                            continue;
                        }

                        // 語・学があった場合は末尾に$を付与(要件No.5)
                        boolean searchStopFlg = linkText.endsWith("語") || linkText.endsWith("学");
                        if (searchStopFlg) {
                            visited.add(linkText);
                            children.add(linkText);
                            continue;
                        }

                        // 語・学で終了していない場合かつ初めてのキーワードなら探索済みセットに保持
                        visited.add(linkText);
                        children.add(linkText);

                        System.out.println(treeDepthMap);

                        queue.add(new Object[] { link.absUrl("href"), linkText, nextDepth });
                    }
                }
            } catch (InterruptedException | IOException e) {
                // 割り込みが発生した場合は処理を中断
                logger.log(Level.SEVERE, "割り込みが発生しました。探索を中断します。", e);
                Thread.currentThread().interrupt();
                return;
            }
            treeMap.put(keyword, children);
        }
    }

    /**
     * 探索したものをツリーとして出力します
     * 
     * @param keyword   出力するキーワード
     * @param depth     現在の深さ
     * @param displayed 表示したキーワードの集合
     */
    public static void printTree(String keyword, int depth, Set<String> displayed) {
        String indent = "    ".repeat(depth);
        String cleanKey = keyword.replaceAll("[@$]+$", "");

        if (cleanKey.endsWith("語") || cleanKey.endsWith("学")) {
            System.out.println(indent + "- " + cleanKey + "$");
            displayed.add(cleanKey); // 既に出現したことにはする
            return; // 探索はしないのでここで終了
        }

        // 表示済みなら@を付与する(要件No.1)
        if (displayed.contains(cleanKey)) {
            System.out.println(indent + "- " + cleanKey + "@");
            return;
        }

        if (depth > treeDepthMap.get(cleanKey)) {
            System.out.println(indent + "- " + cleanKey + "@");
            return;
        }

        displayed.add(cleanKey);

        // 未探索のキーワード(=最大探索回数に漏れたもの)には$を付与(要件No.2)
        boolean isExplored = treeMap.containsKey(cleanKey);
        String suffix = isExplored ? "" : "$";

        // 取得したキーワードは全て表示(要件No.3)
        System.out.println(indent + "- " + keyword + suffix);

        // 子要素がいれば再帰的に表示
        List<String> children = treeMap.get(cleanKey);
        if (children != null) {
            for (String child : children) {
                printTree(child, depth + 1, displayed);
            }
        }
    }
}