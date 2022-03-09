package xzc.server.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import xzc.server.constant.Card;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AliveGame {
    private Long id;
    // 牌库 每轮游戏自动洗牌
    private List<Card> cardHouse;
    //牌库指针 到第几张牌
    private Integer cardHousePointer = 0;
    //第几轮（0-6）
    private Integer round = 0;
    /**
     * 总轮数
     */
    private Integer totalRound = 7;
    /**
     * 起始玩家，上一局的winner, 首轮是第一个加入房间的玩家
     */
    private Long startingGamer;
    /**
     * 本轮玩家的行动顺序，每到新的一轮都会更换起始玩家
     */
    private List<Long> orderedGamerList;
    // 当前行动的玩家 id
    private Long curGamerId;
    // 加入赌局的玩家
    private List<Long> gambler;
    /**
     * 小早川牌
     */
    private Card xzcCard;
    /**
     * 每轮得分记录
     */
    private List<PointBand> pointBandList;
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
        private Card card;
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
        Idle,
        Waiting,
        InAction,
        Picking,
        Betting,

    }

    public enum GamerEvent {
        TakeCard,
        Betting,

    }

    /**
     * 计分板
     */
    public static class PointBand {
        /**
         * 小早川牌点数
         */
        private Integer xzcPoint;
        /**
         * 当前轮次
         */
        private Integer round;
        /**
         * 获胜者
         */
        private Long winner;
        /**
         * 最终得分
         */
        private Map<Long, Integer> finalPointMap;
    }

}
