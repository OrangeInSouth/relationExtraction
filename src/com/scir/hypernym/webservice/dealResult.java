package com.scir.hypernym.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;

import com.scir.hypernym.crawler.BaiduWebClient;

public class dealResult {
	static String FILENAME = "./Resource/input/output_jiang教育.txt";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// 中共中央, 总书记,
		if (args.length >= 1) {
			FILENAME = args[0];
		}
		
		File file = new File(FILENAME);
		BufferedReader reader = null;
		try {
			// System.out.println("以行为单位读取文件内容，一次读一整行：");
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;

			FileOutputStream fos = new FileOutputStream("./Resource/handleResult.txt");
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");

			FileOutputStream fos_score = new FileOutputStream(
					"./Resource/handleResult_score.txt");
			OutputStreamWriter osw_score = new OutputStreamWriter(fos_score,
					"UTF-8");

			BaiduWebClient.Initialize();

			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				// 显示行号
				// System.out.println("line " + line + ": " + tempString);
				// line++;

				Vector<String> queryTuple = preHandle(tempString);

				String query = queryTuple.get(0) + " " + queryTuple.get(2);

				osw.write("(" + queryTuple.get(0) + "," + queryTuple.get(1)
						+ "," + queryTuple.get(2) + ")");
				osw.write("\t");

				osw_score.write("(" + queryTuple.get(0) + ","
						+ queryTuple.get(1) + "," + queryTuple.get(2) + ")");
				osw_score.write("\t");

				if (skip(queryTuple)) {
					System.out.println("条件不符，跳过!");
					osw.write("条件不符，跳过\n");
					osw_score.write("false\n");
					continue;
				}

				System.out.println("query=[" + query + "]");

				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
						DateFormat.FULL);
				Date date = new Date();
				String timeStart = df.format(date);
				System.out.println("timeStart:" + timeStart);

				// -------search in Baidu--------

				// BaiduWebClient.search(q, qbefore, qafter, saigo);
				BaiduWebClient.search(query, queryTuple.get(0),
						queryTuple.get(1), queryTuple.get(2),
						Integer.parseInt(queryTuple.get(3)), osw, osw_score);
				Long time = System.currentTimeMillis() - date.getTime();
				double second = time.doubleValue() / 1000;
				// response.getWriter().printf("%f\n", second);
				System.out.println("usedtime:" + second + "s");
				osw.write("\n");
				osw_score.write("\n");
			}
			osw.close();
			osw_score.close();

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}

		return;
	}

	static private boolean skip(Vector<String> queryTuple) {
		if (queryTuple.size() != 4) {
			return true;
		} else {
			for (int i = 0; i < queryTuple.size() - 1; i++) {
				if (queryTuple.get(i).length() < 2) {
					return true;
				}
			}
			return false;
		}
	}

	static private Vector<String> preHandle(String line) {
		String[] result = line.split("\t");
		Vector<String> saigo = new Vector<String>();
		int cons = 1;
		int cons2 = 3;
		for (int i = 0; i < result.length; i++) {
			if (result[i].equals("")) {
				cons++;
				cons2++;
				cons %= 4;
				cons2 %= 4;
				continue;
			}
			if (i % 4 == cons) {
				String temp = result[i].replaceAll("\\s+", "").replaceAll(" +",
						"");

				temp = temp.replaceAll("\\(", "");
				temp = temp.replaceAll("\\)", "");
				System.out.println(temp);
				String[] tempArr = temp.split(",");
				for (int j = 0; j < tempArr.length; j++) {
					if (tempArr[j].equals("")) {
						continue;

					}
					saigo.add(tempArr[j]);
				}
			}
			if (i % 4 == cons2) {
				int num = Integer.parseInt(result[i]);
				saigo.add(num + "");
			}
		}

		return saigo;
	}
}
