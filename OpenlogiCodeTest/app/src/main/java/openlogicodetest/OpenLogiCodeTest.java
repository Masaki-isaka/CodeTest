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

public class OpenLogiCodeTest {

    /** 開始URL */
    private static final String START_URL = "https://ja.wikipedia.org/wiki/%E3%82%A8%E3%83%AA%E3%82%A6%E3%83%89%E3%83%BB%E3%82%AD%E3%83%97%E3%83%81%E3%83%A7%E3%82%B2";

    /** 開始キーワード */
    private static final String START_KEY_WORD = "エリウド・キプチョゲ";

    /** 最大探索回数(変更する場合はこの値を変えて下さい) */
    private static final int MAX_SEARCH = 20;

    private static final AtomicInteger searchCount = new AtomicInteger(0);

    /** 訪問済みセット */
    private static Set<String> visited = new HashSet<>();

    /** 親子関係を保持するMap(親・子のリスト) */
    private static Map<String, List<String>> treeMap = new LinkedHashMap<>();

    /** 文字列の深さを保持するMap(Key:文字, value:深さ(0始まり)) */
    private static Map<String, Integer> treeDepthMap = new LinkedHashMap<>();

    /** ログ出力のインスタンス化 */
    private static final Logger logger = Logger.getLogger(OpenLogiCodeTest.class.getName());

    /** $ */
    private static final String DOLLAR_SIGN = "$";

    /** @ */
    private static final String SUFFIX_AT = "@";

    /** 語 */
    private static final String GO = "語";

    /** 学 */
    private static final String GAKU = "学";

    /** ハイフン */
    private static final String HYPTHEN = "- ";

    /** 空白 */
    private static final String BLANK = "";

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        // 幅優先的に探索する
        searchAsBfs(START_URL, START_KEY_WORD, 0);

        // 探索したものを深さ優先的に出力する
        printTreeAsDfs(START_KEY_WORD, 0, new HashSet<>());

        System.out.println("\nExecution Time: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * 幅優先的に探索します
     * 
     * @param startUrl     スタートするURL
     * @param startKeyword スタートするキーワード
     * @throws InterruptedException
     */
    public static void searchAsBfs(String startUrl, String startKeyword, int depth) {

        // URL,キーワード,深さ(0始まり)をキューとして保持
        Queue<Object[]> queue = new LinkedList<>();
        queue.add(new Object[] { startUrl, startKeyword, depth });

        // 一度訪問したキーワードを保持
        visited.add(startKeyword);

        treeDepthMap.put(startKeyword, depth);

        // 探索ロジック
        // 幅優先的に探索(要件No.4)
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

                        // 下記で当該キーワードの深さを保持することで、出力時に階層の優先度を判定する
                        int nextDepth = currentDepth + 1;
                        if (!treeDepthMap.containsKey(linkText)) {
                            treeDepthMap.put(linkText, nextDepth);
                        }

                        // キーワード重複時、探索をスキップ(要件No.1)
                        if (visited.contains(linkText)) {
                            children.add(linkText);
                            continue;
                        }

                        // 語・学があった場合は探索を中止する(要件No.5)
                        if (isFixedText(linkText)) {
                            visited.add(linkText);
                            children.add(linkText);
                            continue;
                        }

                        // 語・学で終了していない場合かつ初めてのキーワードなら探索済みセットに保持
                        visited.add(linkText);
                        children.add(linkText);

                        queue.add(new Object[] { link.absUrl("href"), linkText, nextDepth });
                    }
                }
            } catch (InterruptedException e) {
                // 割り込みが発生した場合は処理を中断し、出力する
                logger.log(Level.SEVERE, "割り込みが発生しました。探索を中断します。", e);
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                // 通信エラーが発生した場合は処理を中断し、出力する
                logger.log(Level.SEVERE, "通信エラーが発生しました。探索を中断します。", e);
                return;
            }
            treeMap.put(keyword, children);
        }
    }

    /**
     * 探索したものを深さ探索的にツリーとして出力します
     * 
     * @param keyword   出力するキーワード
     * @param depth     現在の深さ
     * @param displayed 表示したキーワードの集合
     */
    public static void printTreeAsDfs(String keyword, int depth, Set<String> displayed) {
        String indent = "    ".repeat(depth);

        // 語・学があった場合は$を付与する(要件No.5)
        if (isFixedText(keyword)) {
            System.out.println(indent + HYPTHEN + keyword + DOLLAR_SIGN);
            displayed.add(keyword);
            return;
        }

        // 表示済みなら@を付与する(要件No.1)
        if (displayed.contains(keyword)) {
            System.out.println(indent + HYPTHEN + keyword + SUFFIX_AT);
            return;
        }

        // 同じ文字があった場合上位階層を優先して出力(要件No.1,4)
        if (depth > treeDepthMap.get(keyword)) {
            System.out.println(indent + HYPTHEN + keyword + SUFFIX_AT);
            return;
        }

        displayed.add(keyword);

        // 未探索のキーワード(=最大探索回数に漏れたもの)には$を付与(要件No.2)
        boolean isExplored = treeMap.containsKey(keyword);
        String suffix = isExplored ? BLANK : DOLLAR_SIGN;

        // 取得したキーワードは全て表示(要件No.3)
        System.out.println(indent + HYPTHEN + keyword + suffix);

        // 子要素がいれば再帰的に表示
        List<String> children = treeMap.get(keyword);
        if (children != null) {
            for (String child : children) {
                printTreeAsDfs(child, depth + 1, displayed);
            }
        }
    }

    /**
     * 文字の最後に固定の文字が含まれているかのチェックを行います
     * 
     * @param text
     * @return 文字の最後に固定の文字が含まれていた場合 true,そうでなければfalse
     * 
     */
    private static boolean isFixedText(String text) {
        if (text.endsWith(GO) || text.endsWith(GAKU)) {
            return true;
        }
        return false;
    }
}