package start;

import info.FairyInfo;
import info.FloorInfo;

import java.util.Scanner;

import net.Crypto;
import net.Process;

@SuppressWarnings("unused")
public class Go {
	
	public static void main(String []args) throws Exception{
		try{
			GetConfig.readConfig(args[0]);
			Process proc = new Process();
			//System.out.println(Crypto.DecryptBase64NoKey2Str("NzgOGTK08BvkZN5q8XvG6Q"));
			while(true){
				proc.start();
			}
		}catch(Exception e){
			log("Error parsing file");
		}
	}
	
	public static void log(String msg){
		System.out.println(msg);
	}
	
	public static void logWaitRelist() {
		Go.log("No untouched fairy, waiting " + Info.waitTime + " seconds to relist");
	}

	public static void logBattle(Info info) {
		Go.log("Battle result: " + info.battleResult + 
				", got exp: " + info.exp + ", gold: " + info.gold + 
				", AP: " + info.apCurrent + "/" + info.apMax + 
				", BC: " + info.bcCurrent + "/" + info.bcMax +
				", exp to next level: " + info.nextExp);
	}

	public static void logWaitCD() {
		Go.log("Waiting for battle CD...");		
	}
	
	public static void logBeginRunMap() {
		Go.log("Begin running map");
	}

	public static void logStatus(Info info) {
		Go.log(info.userName + ", level: " + info.userLv + 
				", AP: " + info.apCurrent + "/" + info.apMax + 
				", BC: " + info.bcCurrent + "/" + info.bcMax +
				", skill points: "	+ info.freeApBcPoint);
	}

	public static void logFloor(FloorInfo floorInfo, Info info) {
		Go.log(floorInfo.name + ", floor " + info.floorId
				+ "(" + info.progress + "%)"
				+ ", spent AP " + info.floorCost + ", gained exp: "
				+ info.getExp + ", gold: " + info.runGold
				+ ", event: "
				+ info.event_type + ", current AP: "
				+ info.apCurrent + ", current BC: "
				+ info.bcCurrent);
		}

	public static void logFairy(FairyInfo fairyInfo) {
		Go.log("Discovered by " + fairyInfo.userName + ", fairy: " + fairyInfo.name
				+ " level " + fairyInfo.lv 
				+ ", HP: " + fairyInfo.currentHp
				+ "/" + fairyInfo.maxHp);
	}

	
	
}










