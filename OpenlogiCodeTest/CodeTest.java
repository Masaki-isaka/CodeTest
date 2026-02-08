import java.util.LinkedHashMap;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.*;

public class CodeTest {

    /* 旧アルゴリズム */
    public static final String OLD_ALGORITHM = "oldAlgorithm";

    /* 新アルゴリズム */
    public static final String NEW_ALGORITHM = "newAlgorithm";

    /* SHA-256 */
    public static final String SHA_256 = "SHA-256";

    /* インターリービング処理を行うページ件数。要件が変わった場合この値を変えて下さい */
    public static final Integer interleavingPageCount = 1;

    public static final Integer MAX_PAGE = 100;

    public static final Logger LOGGER = Logger.getLogger(CodeTest.class.getName());

    public static final String WARNING_CONTENTS = "SHA-256ハッシュ関数が使用できません";

    public static final String COLON = ":";

    /**
     * @param userId  アクセスしたユーザーのID
     * @param page    ページ番号（1から始まる）
     * @param perPage 1ページあたりの要求するアイテム数（基本は10だが、場合によって異なる）
     * @return [Array<JobPost>] 募集のリスト
     * 
     */
    public List<JobPost> interleaving(Integer userId, Integer page, Integer perPage) {
        // ユーザが100ページ目以降に進もうとしたとき固定メッセージと固定アイテムを表示する(おそらくサービスの仕様)
        if (page > MAX_PAGE) {
            System.out.println("固定メッセージと固定アイテムを返し、以降の処理に進まない");
            return new ArrayList<>();
        }

        // 2ページ分(20件)を一括生成して切り出す。このリストは3ページ目以降をクリックした場合でも除外リストとして使用する
        List<JobPost> fullInterleavingList = this.computeBalancedInterleaving(userId,
                interleavingPageCount * perPage);

        // 3ページ目以降クリック時は以下のロジックを通る
        if (page > interleavingPageCount) {
            List<JobPost> pageItemList = this.selectItemFromOldAlgorithm(userId, page, perPage, interleavingPageCount,
                    fullInterleavingList);
            return pageItemList;
        }

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, fullInterleavingList.size());

        return new ArrayList<>(fullInterleavingList.subList(start, end));
    }

    /**
     * Balanced Interleavingを実装
     * 
     * @param userId      アクセスしたユーザーのID
     * @param totalNeeded インターリービング処理に必要となる総件数
     * @return インターリービング処理を行ったリスト
     */
    private List<JobPost> computeBalancedInterleaving(Integer userId, Integer totalNeeded) {
        long seed = this.createSeedValue(userId);

        // 乱数生成（このシード値からは常に同じランダム列が出る）
        Random rand = new Random(seed);

        // シード値なので同じユーザ、同じ日付の場合は常に同じ順序を返す
        boolean fromOld = rand.nextBoolean();

        // インターリービング対象となる全データを取得
        // 1,2ページ分(20件)を作るには、各アルゴリズムから最大20件ずつあれば足りる
        List<Integer> oldAlgorithmList = this.oldAlgorithm(userId, 1, totalNeeded);
        List<Integer> newAlgorithmList = this.newAlgorithm(userId, 1, totalNeeded);

        int oldAlgorithmPointer = 0;
        int newAlgorithmPointer = 0;

        // LinkedHashMapを使用することで順番を維持しながら値を保持し、計算量も抑える
        Map<Integer, String> map = new LinkedHashMap<>();

        // 生成されるランキングがインターリービングに必要なアイテム数に達するまで処理を行う
        while (map.size() < totalNeeded
                && (oldAlgorithmPointer < oldAlgorithmList.size()
                        || newAlgorithmPointer < newAlgorithmList.size())) {
            if (newAlgorithmPointer > oldAlgorithmPointer
                    || (fromOld && newAlgorithmPointer == oldAlgorithmPointer)) {
                if (!map.containsKey(oldAlgorithmList.get(oldAlgorithmPointer))) {
                    map.put(oldAlgorithmList.get(oldAlgorithmPointer),
                            OLD_ALGORITHM);
                }
                oldAlgorithmPointer++;
            } else {
                if (!map.containsKey(newAlgorithmList.get(newAlgorithmPointer))) {
                    map.put(newAlgorithmList.get(newAlgorithmPointer),
                            NEW_ALGORITHM);
                }
                newAlgorithmPointer++;
            }
        }

        List<JobPost> interleavingList = this.createInterLeavingList(map);

        return interleavingList;
    }

    /**
     * 3ページ目以降クリック時に古いアルゴリズムからアイテムを取得
     * 
     * @param userId                ユーザID
     * @param page                  ページ番号（1から始まる）
     * @param perPage               1ページあたりの要求するアイテム数（基本は10だが、場合によって異なる）
     * @param interleavingPageCount インターリービング対象となるページ件数
     * @param fullInterleavingList  インターリービング処理されたアイテムリスト
     * @return pageItemList 当該ページのアイテムリスト
     * 
     */
    private List<JobPost> selectItemFromOldAlgorithm(Integer userId, Integer page, Integer perPage,
            Integer interleavingPageCount, List<JobPost> fullInterleavingList) {
        // インターリービング処理したIDを除外用リストとして保持
        Set<Integer> excludedIds = fullInterleavingList.stream()
                .map(JobPost::getId)
                .collect(Collectors.toCollection(HashSet::new));

        // 1ページ～当該ページまでの旧アルゴリズムの全アイテムを呼び出し、重複アイテムを除外する。
        List<Integer> totalOldAlgorithmIds = this.oldAlgorithm(userId, 1, page * perPage);

        List<JobPost> pageItemList = totalOldAlgorithmIds.stream()
                .filter(id -> !excludedIds.contains(id)) // 除外処理
                .skip(Math.max(0, (long) (page - interleavingPageCount - 1) * perPage)) // ページ飛ばし
                .limit(perPage) // 1ページ分取得
                .map(id -> { // JobPostオブジェクトに変換
                    JobPost jobPost = new JobPost();
                    jobPost.setId(id);
                    jobPost.setSource(OLD_ALGORITHM);
                    return jobPost;
                })
                .toList();

        return pageItemList;
    }

    /**
     * シード値の生成
     * 
     * @param userId ユーザID
     * @return seed シード値
     */
    private long createSeedValue(Integer userId) {
        long seed;
        try {
            // シード値の固定 (ユーザーID + 今日の日付)
            // シード値をユーザIDにすることで未ログイン時でもユーザ識別子を入れてインターリービング処理が適用されるようにする
            // 本日日付を入れることで1日1回のアルゴリズム更新に対応する。
            StringBuilder sb = new StringBuilder();
            sb.append(userId);
            sb.append(COLON);
            sb.append(LocalDate.now().toString());
            String input = sb.toString();

            // SHA-256ハッシュ関数を使用してユーザIDのわずかな違いでもシード値の結果を変える
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // ハッシュ結果（32バイト）の最初の8バイトをlong型に変換してシードにする
            seed = ByteBuffer.wrap(hash).getLong();
        } catch (NoSuchAlgorithmException e) {
            // アルゴリズムが使えない場合別にシード値を生成
            seed = Objects.hash(userId, LocalDate.now().toString());
            LOGGER.warning(WARNING_CONTENTS);
        }

        return seed;
    }

    /**
     * Mapに格納されたアイテムをリストに格納
     * 
     * @param map インターリービング処理されたMap<ID,新or旧アルゴリズム>
     * @return interleavingList インターリービング処理されたアイテムリスト
     */
    private List<JobPost> createInterLeavingList(Map<Integer, String> map) {
        // インターリービング処理によって新たに生成されるランキング
        List<JobPost> interleavingList = new ArrayList<JobPost>();

        // 生成されたMapをリストに追加する
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            JobPost jobPost = new JobPost();
            jobPost.setId(entry.getKey());
            jobPost.setSource(entry.getValue());
            interleavingList.add(jobPost);
        }

        return interleavingList;
    }

    /**
     * @param [Integer] userId アクセスしたユーザーのID
     * @param [Integer] page ページ番号（1から始まる）
     * @param [Integer] perPage 1ページあたりの要求するアイテム数（基本は10だが、場合によって異なる）
     * @return 募集のIDのリスト
     */
    public List<Integer> oldAlgorithm(Integer userId, Integer page, Integer perPage) {

        // 具体的なランキングアルゴリズムの実装は不要です
        // 適当なIDのリストを返すスタブを実装してください
        // 例:
        List<Integer> oldAlgorithmOriginalIds = Arrays.asList(15, 21, 40, 6, 39, 23, 30, 38, 1, 7, 14, 29, 37, 12, 36,
                24, 18, 10, 13, 20,
                3, 35, 22, 27, 34, 26, 11, 4, 5, 9, 17, 28, 32, 33, 8, 2, 31, 19, 16, 25);

        List<Integer> oldAlgorithmSubListIds = oldAlgorithmOriginalIds.subList((page - 1) * perPage, page * perPage);
        List<Integer> oldAlgorithmPerPageList = new ArrayList<>(oldAlgorithmSubListIds);

        return oldAlgorithmPerPageList;

    }

    /**
     * @param [Integer] userId アクセスしたユーザーのID
     * @param [Integer] page ページ番号（1から始まる）
     * @param [Integer] perPage 1ページあたりの要求するアイテム数（基本は10だが、場合によって異なる）
     * @return 募集のIDのリスト
     */
    public List<Integer> newAlgorithm(Integer userId, Integer page, Integer perPage) {
        // 具体的なランキングアルゴリズムの実装は不要です
        // 適当なIDのリストを返すスタブを実装してください
        // 例:
        List<Integer> newAlgorithmOriginalIds = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40);
        List<Integer> newAlgorithmSubListIds = newAlgorithmOriginalIds.subList((page - 1) * perPage, page * perPage);
        List<Integer> newAlgorithmPerPageList = new ArrayList<>(newAlgorithmSubListIds);

        return newAlgorithmPerPageList;
    }
}
