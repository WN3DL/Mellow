package com.roxiun.mellow.feature.requestpopup;

public enum RequestPopupPosition {
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_RIGHT;

    public static RequestPopupPosition fromConfigIndex(int index) {
        if (index <= 0) {
            return TOP_CENTER;
        }
        if (index == 1) {
            return TOP_RIGHT;
        }
        return BOTTOM_RIGHT;
    }
}
