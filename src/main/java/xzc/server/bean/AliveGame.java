package xzc.server.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import xzc.server.constant.Card;
import xzc.server.util.CardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AliveGame {
    private Long id;
    // 当前轮次，第几轮（0-6）
    private Integer round = 0;
    /**
     * 总轮数
     */
    private Integer totalRound = 7;
    // 牌库 每轮游戏自动洗牌
    private List<Card> cardLibrary = CardUtil.getRandomCardLibrary();
    // 牌库指针
    private int cardLibraryIndex = 0;
    // 弃牌堆
    private List<Card> discardPile = new ArrayList<>();
    /**
     * 本轮玩家的行动顺序，每到新的一轮都会更换起始玩家
     */
    private List<Long> orderlyGamers;
    // 当前行动的玩家 id
    private int orderedGamersIndex;
    // 本轮加入赌局的玩家
    private List<Long> gambler;
    /**
     * 小早川牌
     */
    private Card xzcCard;
    private Map<Long, Gamer> gamerMap;
    private Long LastChangeTime;

    @Data
    @Accessors(chain = true)
    public static class Gamer {

        private Long uid;

        private String nickname;

        private String avatar;

        private GamerState state;

        private GamerEvent event;
        /**
         * 手牌
         */
        private Card handCard;
        /**
         * 摸牌
         */
        private Card drewCard;
        /**
         * 筹码数，初始每人4个
         */
        private Integer coins = 4;

        private Long joinTime;

        private Long lastStateChange;
    }

    public enum GamerState {
        // 等待中
        WAIT,
        // 行动中
        ACTION,
        // 弃牌
        DISCARD,
        // 下注
        BET,
        // 已下注
        IN_BET;
    }

    public enum GamerEvent {
        // 抓牌
        TAKE_CARD,
        // 弃牌
        DISCARD,
        // 变更小早川牌
        CHANGE_XZC_CARD,
        // 下注
        BETTING,
        // 不下注
        PASS;
    }

}
