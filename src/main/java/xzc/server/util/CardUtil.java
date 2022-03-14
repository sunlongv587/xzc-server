package xzc.server.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import xzc.server.constant.Card;

import java.util.Arrays;
import java.util.List;

public class CardUtil {

    public static List<Card> getRandomCardLibrary() {
        Card[] values = Card.values();
        List<Card> cardList = Lists.newLinkedList();
        cardList.addAll(Arrays.asList(values));
        List<Card> result = Lists.newArrayListWithExpectedSize(cardList.size());
        for (int i = cardList.size() - 1; i > 0; i--) {
            int random = RandomUtils.nextInt(0, i);
            result.add(cardList.remove(random));
        }
        result.add(cardList.get(0));
        return result;
    }


    public static void main(String[] args) {
        List<Card> cardHouse = getRandomCardLibrary();
        for (Card card : cardHouse) {
            System.out.println(card);
        }
    }
}
