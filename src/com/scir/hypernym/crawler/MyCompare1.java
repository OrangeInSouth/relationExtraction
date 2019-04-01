package com.scir.hypernym.crawler;

import java.util.Comparator;

public class MyCompare1 implements Comparator<Score> {

	@Override
	public int compare(Score o1, Score o2) {
		// TODO Auto-generated method stub

		if (o1.getScore() > o2.getScore()) {
			// 1表示交换
			return -1;
		} else if (o1.getScore() < o2.getScore()) {
			return 1;
		} else {
			return 0;
		}
	}

}
