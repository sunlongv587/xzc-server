package xzc.server.bean;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.experimental.Accessors;
import xzc.server.proto.XZCCard;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class Game {

    private Long id;
    /**
     * 每轮牌局数据
     * */
    private List<XZCCard> cardHouse = Lists.newArrayListWithCapacity(15); 	// 牌库 每轮游戏自动洗牌
    private Integer cardHousePointer = 0;				//牌库指针 到第几张牌
    private Integer round = 1;						//第几轮（1-7）
//    private Long curGamerID;					//当前玩家 id 0 1 2
    private List<Long> joinGamer = Lists.newArrayListWithCapacity(6);	//加入赌局的玩家 下标为gamerID 0为pass 1为加入
//    private List<Long> gamersCard = Lists.newArrayListWithCapacity(6);	//加入赌局的玩家 下标为gamerID 0为pass 1为加入
//    int[] GamersCard = new int[3];	//玩家手里的牌的点数
    private Integer omiCard;					//小早川牌
    private Map<Long, Gamer> gameMap;
    private Long LastChangeTime;
}
