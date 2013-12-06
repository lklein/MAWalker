package net;

import info.CardConfigInfo;
import info.FairyInfo;
import info.FloorInfo;
import info.NoNameInfo;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import start.Go;
import start.Info;
import start.Think;
import action.ActionRegistry.Action;
import action.BattleAreaBoss;
import action.ChangeCardItems;
import action.FairyBattle;
import action.FairyHistory;
import action.FloorRun;
import action.GetFairyList;
import action.GetFloorInfo;
import action.GetAreaInfo;
import action.GetNoNameList;
import action.LevelUp;
import action.Login;
import action.PVPRedirect;
import action.PvpWithNoName;
import action.ReturnMain;

public class Process {

	public static Connect connect;
	public static Info info;

	public Process() {
		connect = new Connect();
		info = new Info();
	}

	// byte转doc
	public static Document ParseXMLBytes(byte[] in) throws Exception {
		ByteArrayInputStream bais = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			bais = new ByteArrayInputStream(in);
			Document document = builder.parse(bais);
			return document;
		} catch (Exception e) {
			throw e;
		}
	}

	public void start() {
		try {
			execute(Think.doIt(getPossibleAction()));
		} catch (Exception e) {
			if (e.getMessage() != null) {
				Go.log(e.getMessage());
				if (e.getMessage().contains("9000")) {
					try {
						Thread.sleep(1000 * Info.sleepTime);
					} catch (InterruptedException e1) {
						Go.log(e1.getMessage());
					} finally {
						info.events.push(Info.EventType.cookieOutOfDate);
					}
				} else {
					info.events.push(Info.EventType.fairyAppear);
				}
			} else {
				Go.log("No response from server");
				info.events.push(Info.EventType.fairyAppear);
			}
		}
	}

	// 获取action方案
	private static List<Action> getPossibleAction() {
		ArrayList<Action> result = new ArrayList<Action>();
		if (info.events.size() != 0) {
			switch (info.events.pop()) {
			case notLoggedIn:
			case cookieOutOfDate:
				result.add(Action.LOGIN);
				break;
			case fairyCanBattle:
				result.add(Action.PRIVATE_FAIRY_BATTLE);
				break;
			case needFloorInfo:
				result.add(Action.GET_FLOOR_INFO);
				break;
			case fairyAppear:
				result.add(Action.GET_FAIRY_LIST);
				break;
			case needAPBCInfo:
				result.add(Action.GOTO_FLOOR);
				break;
			case levelUp:
				result.add(Action.LV_UP);
			case getNoNameList:
				result.add(Action.GET_NONAME_LIST);
				break;
			case pvp:
				result.add(Action.PVP);
				break;
			case fairyHistory:
				result.add(Action.FAIRY_HISTORY);
				break;
			}
		}
		return result;
	}

	// 执行action
	private void execute(Action action) throws Exception {
		switch (action) {
		case LOGIN:
			try {
				if (Login.run()) {
					Go.log("cookie:" + info.cookie);
					Go.logStatus(info);
					
					info.events.push(Info.EventType.fairyAppear);
				} else {
					info.events.push(Info.EventType.notLoggedIn);
				}
			} catch (Exception ex) {
				info.events.push(Info.EventType.notLoggedIn);
			}
			break;
		case GET_FAIRY_LIST:
			if (info.freeApBcPoint > 0) {
				info.events.push(Info.EventType.levelUp);
				break;
			}
			if (GetFairyList.run()) {
				info.events.push(Info.EventType.fairyHistory);
			} else {
				ReturnMain.run();
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& Info.canRun == 1) {
					Go.logBeginRunMap();
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// BC如果大于设定值则开始点名
				if ((info.bcCurrent >= Info.whenBcMoreThan)
						&& Info.isPVP.equals("1")) {
					Go.log("BC more than " + Info.whenBcMoreThan + ", roll call");
					info.events.push(Info.EventType.getNoNameList);
					break;
				}
				Go.logWaitRelist();
				Go.logStatus(info);
				Thread.sleep(1000 * Info.waitTime);
				info.events.push(Info.EventType.fairyAppear);
			}
			break;
		case GET_NONAME_LIST:
			if (info.freeApBcPoint > 0) {
				info.events.push(Info.EventType.levelUp);
				break;
			}
			if (GetNoNameList.run()) {
				Go.log("Got noname Arthurs");
				info.events.push(Info.EventType.pvp);
			}
			break;
		case PVP:
			ChangeCardItems.run(Info.pvpCard, Info.pvpLr);
			for (NoNameInfo noName : info.noNameList) {
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& Info.canRun == 1) {
					Go.logBeginRunMap();
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// 如果BC不足设定值，则留着BC舔妖精
				if (info.bcCurrent < Info.whenBcMoreThan) {
					Go.log("BC lower than " + Info.whenBcMoreThan + ", stop roll call");
					info.events.push(Info.EventType.fairyAppear);
					break;
				}
				if (PvpWithNoName.run(noName)) {
					if (info.isLvUp) {
						Go.log("Leveled up!");
						info.events.push(Info.EventType.levelUp);
						break;
					} else {
						Go.logBattle(info);
					}
				}
				if (PVPRedirect.run()) {
					Go.log("Refresh PvP list");
				}
				
				Go.logWaitCD();
				Thread.sleep(1000 * 20);
				// 每点名一个人就检测一下是否有妖精出现
				GetFairyList.run();
				for (FairyInfo fairyInfo : info.fairyInfos) {
					FairyHistory.run(fairyInfo);
				}
				if (info.canBattleFairyInfos.size() > 0) {
					info.events.push(Info.EventType.fairyCanBattle);
					break;
				}
				info.events.push(Info.EventType.getNoNameList);
			}
			break;
		case FAIRY_HISTORY:
			for (FairyInfo fairyInfo : info.fairyInfos) {
				FairyHistory.run(fairyInfo);
			}
			ReturnMain.run();
			if (info.canBattleFairyInfos.size() > 0) {
				info.events.push(Info.EventType.fairyCanBattle);
				break;
			} else {
				// 如果ap大于当前地图所需cost则开始跑图
				if (info.apCurrent >= info.floorCost && Info.isRun.equals("1")
						&& info.bcCurrent < Info.stopRunWhenBcMore
						&& Info.canRun == 1) {
					Go.logBeginRunMap();
					info.events.push(Info.EventType.needFloorInfo);
					break;
				}
				// BC如果大于设定值则开始点名
				if ((info.bcCurrent >= Info.whenBcMoreThan)
						&& Info.isPVP.equals("1")) {
					Go.log("BC more than " + Info.whenBcMoreThan + ", roll call");
					info.events.push(Info.EventType.getNoNameList);
					break;
				}
				Go.logWaitRelist();
				Go.logStatus(info);
				Thread.sleep(1000 * Info.waitTime);
			}
			info.events.push(Info.EventType.fairyAppear);
			break;
		case PRIVATE_FAIRY_BATTLE:
			Go.log("Untouched fairies: " + info.canBattleFairyInfos.size());
			for (FairyInfo fairyInfo : info.canBattleFairyInfos) {
				BigDecimal i = BigDecimal.valueOf(fairyInfo.currentHp).divide(
						BigDecimal.valueOf(fairyInfo.maxHp), 2,
						BigDecimal.ROUND_HALF_DOWN);
				
				Go.logFairy(fairyInfo);
				
				int fairyLv = Integer.parseInt(fairyInfo.lv);
				ChangeCardItems.run(Info.wolf, Info.wolfLr);
				// 一般来说，名字带"的"的都是觉醒……中文做utf编码处理，防止乱码
				// 如果是觉醒使用觉醒卡组
				if (URLEncoder.encode(fairyInfo.name, "utf-8").contains(
						"%E7%9A%84")) {
					for (CardConfigInfo cardConfigInfo : Info.cardConfigInfos) {
						if (fairyLv >= cardConfigInfo.lvMin
								&& fairyLv <= cardConfigInfo.lvMax
								&& info.bcCurrent >= cardConfigInfo.cardCost
								&& i.doubleValue() >= cardConfigInfo.hp
								&& cardConfigInfo.wake == 1) {
							ChangeCardItems.run(cardConfigInfo.cardItem,
									cardConfigInfo.cardLr);
							break;
						}
					}
				} else {
					for (CardConfigInfo cardConfigInfo : Info.cardConfigInfos) {
						if (fairyLv >= cardConfigInfo.lvMin
								&& fairyLv <= cardConfigInfo.lvMax
								&& info.bcCurrent >= cardConfigInfo.cardCost
								&& i.doubleValue() >= cardConfigInfo.hp
								&& cardConfigInfo.wake == 0) {
							ChangeCardItems.run(cardConfigInfo.cardItem,
									cardConfigInfo.cardLr);
							break;
						}
					}
				}
				if (FairyBattle.run(fairyInfo)) {
					if (info.isLvUp) {
						Go.log("Leveled up!");
						info.events.push(Info.EventType.levelUp);
						break;
					} else {
						Go.logBattle(info);
					}

				}
				// 如果BC不足2，则等待两分钟
				if (info.bcCurrent < 2) {
					Go.log("BC lower than " + Info.lickCost + ", recovering...");
					Thread.sleep(1000 * 60 * 2);
				} else {
					Go.logWaitCD();
					Thread.sleep(1000 * 20);
				}
			}
			// 重置
			info.canBattleFairyInfos = new ArrayList<FairyInfo>();
			info.events.push(Info.EventType.fairyAppear);
			break;
		case LV_UP:
			if (LevelUp.run()) {
				Process.info.isLvUp = false;
				Process.info.freeApBcPoint = 0;
				Go.log("Free points spent");
				Go.logStatus(info);
			}
			info.events.push(Info.EventType.fairyAppear);
			break;
		case GET_FLOOR_INFO:
			if (GetAreaInfo.run(false)) {
				FloorInfo floorInfo = null;
				boolean isClear = false;
				if (info.floorInfos.size() > 0) {
					if (Info.dayFirst.equals("1") && Info.runFactor.equals("1")) {
						for (FloorInfo fInfo : info.floorInfos) {
							if (fInfo.id.contains("5000")) {
								floorInfo = fInfo;
								break;
							} else {
								floorInfo = info.floorInfos.get(0);
							}
						}
						GetFloorInfo.run(floorInfo, false);
					} else {
						if (Info.whatMap != 0) {
							GetAreaInfo.run(true);
							floorInfo = info.floorInfos.get(Info.whatMap - 1);
							GetFloorInfo.run(floorInfo, true);
						} else {
							floorInfo = info.floorInfos.get(Info.whatMap);
							GetFloorInfo.run(floorInfo, false);
						}
					}
				} else {
					GetAreaInfo.run(true);
					if (Info.whatMap != 0) {
						floorInfo = info.floorInfos.get(Info.whatMap - 1);
					} else {
						floorInfo = info.floorInfos.get(Info.whatMap);
					}
					GetFloorInfo.run(floorInfo, true);
					isClear = true;
				}
				Go.log("Map loaded");
				if (!info.floorId.equals("") && info.floorCost != 0) {
					if (info.apCurrent < info.floorCost) {
						Go.log("Not enough AP");
						info.events.push(Info.EventType.fairyAppear);
						break;
					} else {
						while (FloorRun.run(floorInfo)) {
							Go.logFloor(floorInfo, info);
						
							// 地图踏破则更新楼层
							if (info.progress == 100 && !isClear) {
								info.floorId = info.nextFloorId;
								info.floorCost = info.nextFloorCost;
							}
							// 每跑一个图就检测一下是否有妖精出现
							GetFairyList.run();
							for (FairyInfo fairyInfo : info.fairyInfos) {
								FairyHistory.run(fairyInfo);
							}
							if (info.canBattleFairyInfos.size() > 0) {
								info.events.push(Info.EventType.fairyCanBattle);
								break;
							}
							// 地图clear则重新获取地图
							if (info.areaClear == 1 && !isClear) {
								info.events.push(Info.EventType.needFloorInfo);
								break;
							}
							// ap不足则跳出循环
							if (info.apCurrent < info.floorCost) {
								Go.log("Not enough AP");
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
							// bc超过设定值则跳出循环
							if (info.bcCurrent > Info.stopRunWhenBcMore) {
								Go.log("BC cap reached, stop running map");
								info.events.push(Info.EventType.fairyAppear);
								break;
							}
						}
					}
				} else {
					if (info.bossId != 0) {
						Go.log("Map guardian!");
						ChangeCardItems.run(Info.battleBoss, Info.battleBossLr);
						if (BattleAreaBoss.run(floorInfo)) {

						}
						info.events.push(Info.EventType.needFloorInfo);
						break;
					}
				}
			} else {
				info.events.push(Info.EventType.fairyAppear);
			}
			break;
		}
	}

}
