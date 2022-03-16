package xzc.server.constant;

import xzc.server.proto.game.XzcCard;

public enum Card {
    CRAD_1(1),
    CRAD_2(2),
    CRAD_3(3),
    CRAD_4(4),
    CRAD_5(5),
    CRAD_6(6),
    CRAD_7(7),
    CRAD_8(8),
    CRAD_9(9),
    CRAD_10(10),
    CRAD_11(11),
    CRAD_12(12),
    CRAD_13(13),
    CRAD_14(14),
    CRAD_15(15);

    private int point;

    Card(int point) {
        this.point = point;
    }

    public static Card of(XzcCard xzcCard) {
        for (Card value : values()) {
            if (value.point == xzcCard.getNumber()) {
                return value;
            }
        }
        return null;
    }

    public static XzcCard toXzcCard(Card card) {
        for (XzcCard value : XzcCard.values()) {
            if (card.point == value.getNumber()) {
                return value;
            }
        }
        return XzcCard.UNRECOGNIZED;
    }

}
