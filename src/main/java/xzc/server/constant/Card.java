package xzc.server.constant;

public enum Card {
    CRAD_1(0, 1),
    CRAD_2(1, 2),
    CRAD_3(2, 3),
    CRAD_4(3, 4),
    CRAD_5(4, 5),
    CRAD_6(5, 6),
    CRAD_7(6, 7),
    CRAD_8(7, 8),
    CRAD_9(8, 9),
    CRAD_10(9, 10),
    CRAD_11(10, 11),
    CRAD_12(11, 12),
    CRAD_13(12, 13),
    CRAD_14(13, 14),
    CRAD_15(14, 15);

    private int index;

    private int point;

    Card(int index, int point) {
        this.index = index;
        this.point = point;
    }



}