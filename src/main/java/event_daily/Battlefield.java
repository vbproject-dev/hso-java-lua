package event_daily;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import ai.PlayerClone;
import client.Player;
import core.Manager;
import core.Service;
import core.Util;
import client.io.Message;
import lombok.Data;
import game.map.LeaveItemMap;
import game.map.GameMap;
import game.map.MapService;
import game.map.MobInMap;
import model.map.Vgo;
import template.ItemTemplate3;
import template.MainObject;
import template.MemberBattlefield;
@Data
public class Battlefield {
	private static Battlefield instance;
	private HashMap<String, MemberBattlefield> list;
	private List<MemberBattlefield> BXH;
	private int status; // 0 sleep, 1 : register, 2 : start
	private int time;
	public int[] info_house;
	public List<PlayerClone> list_ai;
	public List<MobInMap> boss;


    public static Battlefield gI() {
		if (instance == null) {
			instance = new Battlefield();
			instance.init();
		}
		return instance;
	}

	private void init() {
		this.list = new HashMap<>();
		this.BXH = new ArrayList<>();
		this.status = 0;
		this.time = 0;
		list_ai = PlayerClone.init();
		this.boss = new ArrayList<>();
		//
	}

	public synchronized void update() {
		try {
			if (this.status == 1) {
				this.time--;
				if (this.time <= 0) {
					this.start();
				}
			} else if (this.status == 2) {
				this.time--;
				// event
				if (this.time == 60 * 50) {
					create_boss(10);
				} else if (this.time == 60 * 40) {
					create_boss(20);
				}
				//
				if (this.time <= 0) {
					this.finish();
				}
			}
		} catch (IOException e) {
		}
	}

	private void create_boss(int i) {
		if (i == 20) {
			MobInMap m = null;
			for (int j = 0; j < boss.size(); j++) {
				if (boss.get(j).is_boss_active) {
					m = boss.get(j);
					break;
				}
			}
			if (m == null) {
				int index = Util.random(boss.size());
				if (!boss.get(index).is_boss_active && boss.get(index).level == 10) {
					boss.get(index).level = 100;
					boss.get(index).is_boss_active = true;
				}
				try {
                    Manager.gI().chatKTGprocess(" The Snake Queen has appeared on the battlefield.");

                } catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				for (int j = 0; j < boss.size(); j++) {
					if (!boss.get(j).equals(m) && boss.get(j).zone_id == m.zone_id) {
						boss.get(j).level = 100;
						boss.get(j).is_boss_active = true;
						break;
					}
				}
				try {
                    Manager.gI().chatKTGprocess(" The Snake Queen has appeared on the battlefield.");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			int index = Util.random(boss.size());
			if (!boss.get(index).is_boss_active && boss.get(index).level == 10) {
				boss.get(index).level = 50;
				boss.get(index).is_boss_active = true;
			}
			try {
                Manager.gI().chatKTGprocess(" The Snake Queen has appeared on the battlefield.");

            } catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void start() throws IOException {
		if (this.status == 1) {
            Manager.gI().chatKTGprocess("Pertempuran telah dimulai, silakan pergi ke NPC MR. Ballard untuk mendaftar.");

            this.BXH.clear();
			this.status = 2;
			this.time = 60 * 60;
			int init_house = this.list.size() / 40;
			init_house = init_house > 0 ? init_house : 1;
			this.info_house = new int[] {init_house, init_house, init_house, init_house
					// Map.get_map_by_id(56)[0].maxzone, Map.get_map_by_id(60)[0].maxzone,
					// Map.get_map_by_id(58)[0].maxzone, Map.get_map_by_id(54)[0].maxzone
			};
			//
			List<MemberBattlefield> list = new ArrayList<>();
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				list.add(en.getValue());
			}
			Collections.shuffle(list);
			for (int i = 0; i < list.size(); i++) {
				list.get(i).village = ((i % 4) + 2);
			}
			if (list.size() == 1) {
				list.get(0).village = Util.random(4) + 2;
			}
			if (list.size() < 4) {
                for (MemberBattlefield memberBattlefield : list) {
                    this.info_house[memberBattlefield.village - 2]++;
                }
				for (int i = 0; i < this.info_house.length; i++) {
					this.info_house[i]--;
				}
			}
			// for (int mem : this.info_p) {
			// System.out.print(mem + " ");
			// }
			// System.out.println();
			list.clear();
			//
			byte[] id_map = new byte[] {54, 56, 58, 60};
			for (int i = 0; i < id_map.length; i++) {
				GameMap[] mapp = GameMap.getMapById(id_map[i]);
				for (GameMap gameMap : mapp) {
					for (MobInMap mobb : gameMap.mobs) {
						mobb.isdie = false;
					}
				}
			}
			for (PlayerClone temp : this.list_ai) {
				temp.isdie = false;
				temp.hp = temp.hp_max;
			}
			//
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				// System.out.println(en.getKey() + " " + en.getValue().village);
				Player p0 = GameMap.get_player_by_name(en.getKey());
				if (p0 != null) {
					Vgo vgo = new Vgo();
					switch (en.getValue().village) {
						case 2: { // lang gio
							vgo.toMap = 55;
							vgo.toX = 224;
							vgo.toY = 256;
							MapService.changeFlag(p0.map, p0, 2);
							break;
						}
						case 3: { // lang lua
							vgo.toMap = 59;
							vgo.toX = 240;
							vgo.toY = 224;
							MapService.changeFlag(p0.map, p0, 1);
							break;
						}
						case 4: { // lang set
							vgo.toMap = 57;
							vgo.toX = 264;
							vgo.toY = 272;
							MapService.changeFlag(p0.map, p0, 4);
							break;
						}
						default: { // 5 lang anh sang
							vgo.toMap = 53;
							vgo.toX = 276;
							vgo.toY = 246;
							MapService.changeFlag(p0.map, p0, 5);
							break;
						}
					}
					p0.changeMap(p0, vgo);
				}
			}
		}
	}

	public synchronized void update_house_die(short id) throws IOException {
		switch (id) {
			case 89: { // nha set type 4
				if (this.info_house[2] > 0) {
					this.info_house[2]--;
				}
				break;
			}
			case 90: { // nha gio type 2
				if (this.info_house[0] > 0) {
					this.info_house[0]--;
				}
				break;
			}
			case 91: { // nha anh sang type 5
				if (this.info_house[3] > 0) {
					this.info_house[3]--;
				}
				break;
			}
			case 92: { // nha lua type 3
				if (this.info_house[1] > 0) {
					this.info_house[1]--;
				}
				break;
			}
		}
		for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
			Player p0 = GameMap.get_player_by_name(en.getKey());
			if (p0 != null) {
				Battlefield.gI().send_info(p0);
			}
		}
		int dem = 4;
		for (int i = 0; i < info_house.length; i++) {
			if (info_house[i] <= 0) {
				finish_house(i + 2);
				dem--;
			}
		}
		if (dem <= 1) {
			this.time = 5;
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				Player p0 = GameMap.get_player_by_name(en.getKey());
				if (p0 != null && GameMap.isBattlefieldMap(p0.map.mapId)) {
					p0.updatePointArena(100);
					Battlefield.gI().send_info(p0);
					this.update_time(p0);
				}
			}
			// try {
			// this.finish();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		}
	}

	private void finish_house(int i) {
		try {
			List<String> list_remove = new ArrayList<>();
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				if (en.getValue().village == i) {
					list_remove.add(en.getKey());
					Player p0 = GameMap.get_player_by_name(en.getKey());
					if (p0 != null) {
						Vgo vgo = new Vgo();
						vgo.toMap = 1;
						vgo.toX = 432;
						vgo.toY = 354;
						p0.changeMap(p0, vgo);
						MapService.changeFlag(p0.map, p0, -1);
					}
				}
			}
			list_remove.forEach(l -> {
				this.list.remove(l);
			});
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				Player p0 = GameMap.get_player_by_name(en.getKey());
				if (p0 != null && GameMap.isBattlefieldMap(p0.map.mapId)) {
					Battlefield.gI().send_info(p0);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void finish() throws IOException {
		if (this.status == 2) {
            Manager.gI().chatKTGprocess("Battlefield telah berakhir");

            //
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {

				Player p0 = GameMap.get_player_by_name(en.getKey());
				if (p0 != null) {
					Vgo vgo = new Vgo();
					vgo.toMap = 1;
					vgo.toX = 432;
					vgo.toY = 354;
					p0.changeMap(p0, vgo);
					MapService.changeFlag(p0.map, p0, -1);
				}
			}
			for (int i = 0; i < boss.size(); i++) {
				boss.get(i).level = 10;
				boss.get(i).hp = 0;
				boss.get(i).is_boss_active = false;
				boss.get(i).isdie = true;
			}
			for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
				this.BXH.add(en.getValue());
			}
			this.BXH.sort(new Comparator<MemberBattlefield>() {
				@Override
				public int compare(MemberBattlefield o1, MemberBattlefield o2) {
					return o1.point > o2.point ? -1 : 1;
				}
			});
			//
			this.list.clear();
			this.status = 0;
			this.time = 0;
			this.info_house = null;
		}
	}

	public synchronized void register(Player p) throws IOException {
        if (this.list.containsKey(p.name)) {
            Service.send_notice_box(p.conn, "Sudah terdaftar");
        } else {
            MemberBattlefield temp = new MemberBattlefield();
            temp.name = p.name;
            temp.point = 0;
            temp.village = 0;
            temp.received = false;
            this.list.put(p.name, temp);
            Service.send_notice_box(p.conn, "Pendaftaran berhasil");
        }
	}

	public synchronized void open_register() throws IOException {
		if (this.status == 0) {
            Manager.gI().chatKTGprocess("Pendaftaran pertempuran dibuka");

            this.status = 1;
			this.time = 60;
		}
	}

	public synchronized void send_info(Player p) throws IOException {
		if (this.status != 2 || this.info_house == null) {
			return; // battle not running (ended/being reset by update() in another thread)
		}
		this.update_time(p);
		for (int i = 2; i < 6; i++) {
			Message m = new Message(-94);
			m.writer().writeByte(i); //
			m.writer().writeByte(this.info_house[i - 2]); // total house
			m.writer().writeShort(total_p_of_house(i)); // total p
			m.writer().writeByte(1);
			p.conn.addmsg(m);
			m.cleanup();
		}
	}

	private int total_p_of_house(int i) {
		int result = 0;
		for (Entry<String, MemberBattlefield> en : this.list.entrySet()) {
			if (en.getValue().village == i) {
				Player p0 = GameMap.get_player_by_name(en.getKey());
				if (p0 != null && GameMap.isBattlefieldMap(p0.map.mapId)) {
					result++;
				}
			}
		}
		return result;
	}

	private void update_time(Player p) throws IOException {
		Message m = new Message(-94);
		m.writer().writeByte(-1); //
		m.writer().writeByte(0);
		m.writer().writeShort(0);
		m.writer().writeByte(0);
		m.writer().writeLong(System.currentTimeMillis() - (60 * 60 - this.time) * 1000L);
		p.conn.addmsg(m);
		m.cleanup();
	}

	public void get_ai(Player p, int id) throws IOException {
		for (int i = 0; i < this.list_ai.size(); i++) {
			PlayerClone temp = this.list_ai.get(i);
			if (temp.id == ((short) id)) {
				Message m = new Message(5);
				m.writer().writeShort(temp.id);
                m.writer().writeUTF("Penjaga");

                m.writer().writeShort(temp.x);
				m.writer().writeShort(temp.y);
				m.writer().writeByte(1); // clazz
				m.writer().writeByte(-1);
				m.writer().writeByte(0); // head
				m.writer().writeByte(8); // eye
				m.writer().writeByte(8); // hair
				m.writer().writeShort(50); // level
				m.writer().writeInt(temp.hp); // hp
				m.writer().writeInt(temp.hp_max); // hp max
				m.writer().writeByte(0); // type
				m.writer().writeShort(0); // point pk
				m.writer().writeByte(3); // size part
				//
				byte[] part_ = new byte[] {8, 0, 1};
				for (int j = 0; j < 3; j++) {
					m.writer().writeByte(part_[j]);
					m.writer().writeByte(0);
					m.writer().writeByte(3);
					m.writer().writeShort(-1);
					m.writer().writeShort(-1);
					m.writer().writeShort(-1);
					m.writer().writeShort(-1); // eff
				}
				//
				m.writer().writeShort(-1); // clan
				m.writer().writeByte(-1); // pet
				m.writer().writeByte(7);
				for (int i1 = 0; i1 < 7; i1++) {
					m.writer().writeByte(-1);
				}
				//
				m.writer().writeShort(-1);
				m.writer().writeByte(-1); // type use mount
				m.writer().writeBoolean(false);
				m.writer().writeByte(1);
				m.writer().writeByte(0);
				m.writer().writeShort(-1); // mat na
				m.writer().writeByte(1); // paint mat na trc sau
				m.writer().writeShort(-1); // phi phong
				m.writer().writeShort(-1); // weapon
				m.writer().writeShort(-1);
				m.writer().writeShort(-1); // hair
				m.writer().writeShort(-1); // wing
				m.writer().writeShort(-1); // body
				m.writer().writeShort(-1); // leg
				m.writer().writeShort(-1); // bienhinh
				p.conn.addmsg(m);
				m.cleanup();
				break;
			}
		}
	}

  public static void Obj_Die(GameMap gameMap, MainObject mainAtk, MainObject focus)throws IOException{
        if(!mainAtk.isPlayer() || !focus.isMob())return;
        Player p = (Player)mainAtk;
        MobInMap mob = (MobInMap)focus;
        if (mob != null) {
            // roi do boss co dinh
            short[] id_item_leave3 = new short[]{};
            short[] id_item_leave4 = new short[]{};
            short[] id_item_leave7 = new short[]{};
            //short id_medal_material = -1;
            short sizeRandomMedal = 0;
            switch (mob.template.mob_id) {
                case 89: { 
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if(Util.random(100)<10)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = (short) (50);
                    break;
                }
                case 90: { 
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if(Util.random(100)<20)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 91: { 
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if(Util.random(100)<20)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = (short) (60);
                    break;
                }
                case 92: { 
                    id_item_leave4 = new short[]{-1, -1, -1, -1, -1, -1, 54, 53, 18};
                    id_item_leave7 = new short[]{11, 13, 2, 3, 2, 3, 14};
                    if(Util.random(100)<20)
                        id_item_leave3 = new short[]{(short) Util.random(4577, 4585)};
                    sizeRandomMedal = (short) (60);
                    break;
                }
            }
            for (short id : id_item_leave3) {
                ItemTemplate3 temp = ItemTemplate3.item.get(id);
                LeaveItemMap.leave_item_by_type3(gameMap, id, temp.getColor(), p, temp.getName(), mob.objectId);
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave4) {
                    if (id == -1) {
                        LeaveItemMap.leave_gold(gameMap, mob, p);
                    } else {
                        LeaveItemMap.leave_item_by_type4(gameMap, id, p, mob.objectId,p.objectId);
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                for (short id : id_item_leave7) {
                    LeaveItemMap.leave_item_by_type7(gameMap, id, p, mob.objectId,p.objectId);
                }
            }
            for (int l = 0; l < sizeRandomMedal; l++) {
                LeaveItemMap.leave_item_by_type7(gameMap, (short) Util.random(136, 146), p, mob.objectId,p.objectId);
            }
        }
    }
	public MemberBattlefield get_infor_register(String name) {
		return this.list.get(name);
	}

	public MemberBattlefield get_bxh(String name) {
		for (int i = 0; i < this.BXH.size(); i++) {
			if (this.BXH.get(i).name.equals(name)) {
				if (i < 10) {
					return this.BXH.get(i);
				}
			}
		}
		return null;
	}

	public int get_index_bxh(MemberBattlefield temp) {
		return this.BXH.indexOf(temp);
	}
}