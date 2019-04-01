package edu.hit.ir.ltp4j;

import java.util.ArrayList;
import java.util.List;

import com.scir.hypernym.config.*;

public class ltpTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			CommonConfig.Initialize();
			String cws = CommonConfig.getString("cws");

			if (Segmentor.create(cws) < 0) {
				System.err.println("load failed");
			}

			String sent = "应新西兰总理比尔·英格利希邀请";

			List<String> li = new ArrayList<String>();

			int size = Segmentor.segment(sent, li);

			for (int i = 0; i < size; i++) {
				System.out.print(li.get(i));
				if (i == size - 1) {
					System.out.println();
				} else {
					System.out.print("\t");
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
