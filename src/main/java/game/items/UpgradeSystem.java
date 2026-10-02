package game.items;

import core.Util;
import template.Item3;
import template.Option;

public class UpgradeSystem {

    public static void updateWingUpgradeOption(Item3 item) {
        item.tier++;
        Option option;
        switch (item.tier) {
            case 5 -> {
                item.op.add(new Option(14, Util.random(70, 100), item.id));
            }
            case 10 -> {
                item.part++;
                option = new Option(1, 1, item.id);
                switch (item.id) {
                    case 2894, 2929, 2908, 2880: {
                        option.id = 7;
                        break;
                    }
                    case 2887: {
                        option.id = 9;
                        break;
                    }
                    case 2901: {
                        option.id = 11;
                        break;
                    }
                    case 2915: {
                        option.id = 8;
                        break;
                    }
                    case 2922: {
                        option.id = 10;
                        break;
                    }
                }
                option.setParam(Util.random(100, 301));
                item.op.add(option);

            }
            case 11 -> {
                item.part++;
                option = new Option(1, 1, item.id);
                switch (item.id) {
                    case 2894: {
                        option.id = 11;
                        break;
                    }
                    case 2929: {
                        option.id = 10;
                        break;
                    }
                    case 2908: {
                        option.id = 8;
                        break;
                    }
                    case 2880: {
                        option.id = 9;
                        break;
                    }
                    case 2887, 2901, 2915, 2922: {
                        option.id = 7;
                        break;
                    }
                }
                option.setParam(Util.random(100, 301));
                item.op.add(option);

            }
            case 15 -> {
                option = new Option(1, 1, item.id);
                switch (item.id) {
                    case 2894:
                    case 2929:
                    case 2908:
                    case 2887: {
                        option.id = 28;
                        break;
                    }
                    case 2901:
                    case 2922:
                    case 2880:
                    case 2915: {
                        option.id = 27;
                        break;
                    }
                }
                option.setParam(Util.random(100, 301));
                item.op.add(option);

            }
            case 20 -> {
                item.part++;
                item.op.add(new Option(15, Util.random(100, 301), item.id));

            }
            case 25 -> {
                option = new Option(1, 1, item.id);
                switch (item.id) {
                    case 2894:
                    case 2887: {
                        option.id = 33;
                        break;
                    }
                    case 2929:
                    case 2908: {
                        option.id = 36;
                        break;
                    }
                    case 2901:
                    case 2922: {
                        option.id = 34;
                        break;
                    }
                    case 2880:
                    case 2915: {
                        option.id = 35;
                        break;
                    }
                }
                option.setParam(Util.random(100, 201));
                item.op.add(option);

            }
            case 30 -> {
                item.part++;
                option = new Option(1, 1, item.id);
                switch (item.id) {
                    case 2894:
                    case 2929:
                    case 2908:
                    case 2887: {
                        option.id = 37;
                        item.op.add(new Option(5, Util.random(50, 100)));
                        break;
                    }
                    case 2901:
                    case 2922:
                    case 2880:
                    case 2915: {
                        option.id = 38;
                        item.op.add(new Option(6, Util.random(50, 100)));
                        break;
                    }
                }
                option.setParam(1);
                item.op.add(option);

            }
        }
    }
}
