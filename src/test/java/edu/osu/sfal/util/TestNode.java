package edu.osu.sfal.util;

import edu.osu.lapis.Flags;
import edu.osu.lapis.LapisApi;
import edu.osu.lapis.util.Sleep;

public class TestNode {
	public static void main(String[] args) {
		LapisApi lapisApi = new LapisApi("test1", "http://localhost:8910", "http://localhost:8777");
		lapisApi.publishReadOnly("SIMULATION_FUNCTION_NAME", "test.1");
		double[] readyToCalculate = Flags.getFlag(false);
		lapisApi.publish("readyToCalculate", readyToCalculate);
		double[] finishedCalculating = Flags.getFlag(true);
		lapisApi.publishReadOnly("finishedCalculating", finishedCalculating);
		lapisApi.ready();

		while(true) {
			while(!Flags.evaluateFlagValue(readyToCalculate)) {
				System.out.println();
				Sleep.sleep(2000);
			}
			Flags.setFlagFalse(finishedCalculating);
			Flags.setFlagFalse(readyToCalculate);

			System.out.println("would do calculation here...");

			Sleep.sleep(2500);

			Flags.setFlagTrue(finishedCalculating);
		}

	}
}
