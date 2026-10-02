
package utils;

import java.util.ArrayList;
import java.util.List;
import template.Item3;
import template.Option;

public class ItemInlay {

    public static boolean isInlayGem(short id) {

        return (id >= 382 && id <= 386)
                || (id >= 387 && id <= 391)
                || (id >= 392 && id <= 396)
                || (id >= 397 && id <= 401)
                || (id >= 402 && id <= 406)
                || (id >= 407 && id <= 411)
                || (id >= 412 && id <= 416) // 412-416 là tinh luyện tâm linh, nhưng không có op riêng nên vẫn tính là gem
                || (id >= 23 && id <= 27)
                || (id >= 28 && id <= 32)
                || (id >= 34 && id <= 38)
                || (id >= 39 && id <= 43);
    }

    public static boolean inlayGem(short gemId, Item3 tem) {
        Option ops = getOptions(gemId, tem.id);
        if(ops == null)
            return false;
        for(Option o2 : tem.op)
        {
            if(ops.id == o2.id)
            {
                if(ops.id == 100 || ops.id == 101)
                    return false;
                o2.setParam(o2.getParam(0) + ops.getParam(0));
                return true;
            }
        }
        tem.op.add(ops);
        if((gemId >= 367 && gemId <= 371)||(gemId >= 397 && gemId <= 401)) //phong ma
        {
            tem.op.addAll(GetOps_PhongMa(gemId,tem.id));
        }
        return true;
    }

    private static Option getOptions(short id, short idItem) {

        switch (id) {

            // hỗn nguyên 352–356
            case 352, 353, 354, 355, 356 -> {
                int pr = (357 - id) * 5;
                return new Option(100, pr, idItem);
            }

            // hỗn nguyên tinh luyện 382–386
            case 382, 383, 384, 385, 386 -> {
                int pr = (387 - id) * 5;
                return new Option(100, pr, idItem);
            }

            // khải hoàn 357–361
            case 357, 358, 359, 360, 361 -> {
                int pr = (362 - id) * 5;
                return new Option(101, pr, idItem);
            }

            // khải hoàn tinh luyện 387–391
            case 387, 388, 389, 390, 391 -> {
                int pr = (392 - id) * 5;
                return new Option(101, pr, idItem);
            }

            // lục bảo 362–366
            case 362, 363, 364, 365, 366 -> {
                int idx = 367 - id;
                int pr = switch (idx) {
                    case 1 -> 350;
                    case 2 -> 250;
                    case 3 -> 150;
                    case 4 -> 100;
                    default -> 50;
                };
                return new Option(102, pr, idItem);
            }

            // lục bảo tinh luyện 392–396
            case 392, 393, 394, 395, 396 -> {
                int idx = 397 - id;
                int pr = switch (idx) {
                    case 1 -> 350;
                    case 2 -> 250;
                    case 3 -> 150;
                    case 4 -> 100;
                    default -> 50;
                };
                return new Option(102, pr, idItem);
            }

            // phong ma 367–371
            case 367, 368, 369, 370, 371 -> {
                int idx = 372 - id;
                int pr1 = switch (idx) {
                    case 1 -> 350;
                    case 2 -> 250;
                    case 3 -> 150;
                    case 4 -> 100;
                    default -> 50;
                };
                return new Option(103, pr1, idItem);
            }

            // phong ma tinh luyện 397–401
            case 397, 398, 399, 400, 401 -> {
                int idx = 402 - id;
                int pr1 = switch (idx) {
                    case 1 -> 450;
                    case 2 -> 350;
                    case 3 -> 250;
                    case 4 -> 200;
                    default -> 150;
                };
                return new Option(103, pr1, idItem);
            }

            // sinh mệnh 372–376
            case 372, 373, 374, 375, 376 -> {
                int idx = 377 - id;
                int pr = switch (idx) {
                    case 1 -> 450;
                    case 2 -> 350;
                    case 3 -> 300;
                    case 4 -> 250;
                    default -> 200;
                };
                return new Option(106, pr, idItem);
            }

            // sinh mệnh tinh luyện 402–406
            case 402, 403, 404, 405, 406 -> {
                int idx = 407 - id;
                int pr = switch (idx) {
                    case 1 -> 450;
                    case 2 -> 350;
                    case 3 -> 300;
                    case 4 -> 250;
                    default -> 200;
                };
                return new Option(106, pr, idItem);
            }

            // Tâm linh 377–381
            case 377, 378, 379, 380, 381 -> {
                int idx = 382 - id;
                int pr = switch (idx) {
                    case 1 -> 1400;
                    case 2 -> 900;
                    case 3 -> 700;
                    case 4 -> 500;
                    default -> 300;
                };
                return new Option(107, pr, idItem);
            }

            // Tâm linh tinh luyện 407–411
            case 407, 408, 409, 410, 411 -> {
                int idx = 412 - id;
                int pr = switch (idx) {
                    case 1 -> 1400;
                    case 2 -> 900;
                    case 3 -> 700;
                    case 4 -> 500;
                    default -> 300;
                };
                return new Option(107, pr, idItem);
            }

            // Kerusakan Cahaya
            case 23,24,25,26,27 -> {
                int idx = 28 - id;
                int pr = switch (idx) {
                    case 1 -> 1600;
                    case 2 -> 800;
                    case 3 -> 400;
                    case 4 -> 200;
                    default -> 100;
                };
                return new Option(6, pr, idItem);
            }

            // Kerusakan Kegelapan
            case 28,29,30,31,32 -> {
                int idx = 33 - id;
                int pr = switch (idx) {
                    case 1 -> 1600;
                    case 2 -> 800;
                    case 3 -> 400;
                    case 4 -> 200;
                    default -> 100;
                };
                return new Option(5, pr, idItem);
            }

            // Resitensi Kegelapan
            case 34,35,36,37,38 -> {
                int idx = 39 - id;
                int pr = switch (idx) {
                    case 1 -> 800;
                    case 2 -> 400;
                    case 3 -> 200;
                    case 4 -> 100;
                    default -> 50;
                };
                return new Option(21, pr, idItem);
            }

            // Resitensi Cahaya
            case 39,40,41,42,43 -> {
                int idx = 44 - id;
                int pr = switch (idx) {
                    case 1 -> 800;
                    case 2 -> 400;
                    case 3 -> 200;
                    case 4 -> 100;
                    default -> 50;
                };
                return new Option(22, pr, idItem);
            }
            case 412, 413, 414, 415, 416 -> {
                int idx = 417 - id; // 5, 4, 3, 2, 1
                int pr = switch (idx) {
                    case 1 -> 900;  // lv5: combo 9%
                    case 2 -> 700;  // lv4: combo 7%
                    case 3 -> 500;  // lv3: combo 5%
                    case 4 -> 300;  // lv2: combo 3%
                    default -> 100; // lv1: combo 1%
                };
                return new Option(116, pr, idItem); // ganti ID_COMBO sesuai option ID combo
            }

            default -> {
                return null;
            }
        }
    }


    public static List<Option> GetOps_PhongMa(short id, short idItem)
    {
        List<Option> re = new ArrayList<>();
        if((id >= 367 && id <= 371)||(id >= 397 && id <= 401)) //phong ma
        {
            if(id >= 367 && id <= 371) // phong ma
            {
                int idx = (372 - id);
                int pr1 = idx == 1 ? 350 : (idx == 2 ? 250 : (idx == 3 ? 150 : (idx == 4 ? 100 : 50)));
                int pr2 = idx == 1 ? 700 : (idx == 2 ? 500 : (idx == 3 ? 400 : (idx == 4 ? 300 : 200)));
                int pr3 = idx == 1 ? 100 : (idx == 2 ? 70 : (idx == 3 ? 50 : (idx == 4 ? 20 : 10)));
    //            re.add(new Option(103, pr1, idItem));
                re.add(new Option(104, pr2, idItem));
                re.add(new Option(105, pr3, idItem));
            }
            else if(id >= 397 && id <= 401) // phong ma tinh luyện
            {
                int idx = (402 - id);
                int pr1 = idx == 1 ? 450 : (idx == 2 ? 350 : (idx == 3 ? 250 : (idx == 4 ? 200 : 150)));
                int pr2 = idx == 1 ? 7000 : (idx == 2 ? 5000 : (idx == 3 ? 4000 : (idx == 4 ? 3000 : 2000)));
                int pr3 = idx == 1 ? 1000 : (idx == 2 ? 700 : (idx == 3 ? 500 : (idx == 4 ? 200 : 100)));
    //            re.add(new Option(103, pr1, idItem));
                re.add(new Option(104, pr2, idItem));
                re.add(new Option(105, pr3, idItem));
            }
        }
        return re;
    }
}
