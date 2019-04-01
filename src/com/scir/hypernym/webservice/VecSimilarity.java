package com.scir.hypernym.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.scir.hypernym.config.CommonConfig;

import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;

public class VecSimilarity {
	HashMap<String, Double[]> wordembedding = null;
	static int DIM = 100;

	public VecSimilarity(String dim, String input) {
		wordembedding = new HashMap<String, Double[]>();

		DIM = Integer.parseInt(dim);

		File file = new File(input);

		BufferedReader reader = null;

		System.out.println("加载wordembedding！");
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;

			// 一次读入一行，直到读入null为文件结束
			int sign = 0;
			int total = 0;
			int word_dim = 0;
			int i = 0;
			while ((tempString = reader.readLine()) != null) {
				if (sign == 0) {
					sign = 1;
					continue;
				}
				// System.out.println(tempString);
				handleLine(tempString);
				System.out.println("读取了" + i + "行");
				i++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleLine(String line) {
		Double[] embeding = new Double[DIM];
		String[] items = line.split(" ");
		int sign = 0;
		String word = null;
		int idx = 0;
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals("")) {
				continue;
			}
			if (sign == 0) {
				sign = 1;
				word = items[i];

			} else {
				embeding[idx++] = Double.parseDouble(items[i]);
			}
		}
		wordembedding.put(word, embeding);
		if (idx != DIM) {
			wordembedding.remove(word);
			System.out.println(word + ":维数出错");
		}
	}

	// public Double[] getWordembedding(String word) {
	// if (wordembedding.containsKey(word)) {
	// return wordembedding.get(word);
	// } else {
	// ArrayList<String> segResult = new ArrayList<String>();
	// Segmentor.segment(word, segResult);
	// if (segResult.size() == 1) {
	// System.out.println(word + "不存在");
	// return null;
	// }
	// Double[] result = new Double[DIM];
	// int notin = 0;
	// int sign = 0;
	// for (int i = 0; i < segResult.size(); i++) {
	// if (!wordembedding.containsKey(segResult.get(i))) {
	// System.err.print("不包含" + segResult.get(i) + "\t");
	// notin++;
	// continue;
	// } else {
	// Double[] temp = wordembedding.get(segResult.get(i));
	// for (int j = 0; j < DIM; j++) {
	// if (sign == 0) {
	// result[j] = temp[j];
	// } else {
	// result[j] += temp[j];
	// }
	// }
	// sign = 1;
	// }
	// }
	// if (segResult.size() - notin == 0) {
	// System.out.println(word + "不存在");
	// return null;
	// }
	// for (int i = 0; i < segResult.size(); i++) {
	// result[i] /= (segResult.size() - notin);
	// }
	// return result;
	// }
	// }

	public Double[] getWordembedding(String word, int type) {

		String headword = getHead(word, type);
		word = headword;

		if (wordembedding.containsKey(word)) {
			return wordembedding.get(word);
		} else {
			System.err.print(word + "开始拼凑\t");
			ArrayList<String> segResult = new ArrayList<String>();
			Segmentor.segment(word, segResult);
			if (segResult.size() == 1) {
				System.out.println(word + "不存在");
				return null;
			}
			Double[] result = new Double[DIM];
			int notin = 0;
			int sign = 0;
			for (int i = 0; i < segResult.size(); i++) {
				if (!wordembedding.containsKey(segResult.get(i))) {
					System.err.print("不包含" + segResult.get(i) + "\t");
					notin++;
					continue;
				} else {
					Double[] temp = wordembedding.get(segResult.get(i));
					for (int j = 0; j < DIM; j++) {
						if (sign == 0) {
							result[j] = temp[j];
						} else {
							result[j] += temp[j];
						}
					}
					sign = 1;
				}
			}
			if (segResult.size() - notin == 0) {
				System.out.println(word + "不存在");
				return null;
			}
			for (int i = 0; i < segResult.size(); i++) {
				result[i] /= (segResult.size() - notin);
			}
			return result;
		}
	}

	private String getHead(String word, int type) {
		ArrayList<String> segResult = new ArrayList<String>();
		Segmentor.segment(word, segResult);
		ArrayList<String> tagResult = new ArrayList<String>();
		Postagger.postag(segResult, tagResult);

		if (type == 1) {
			if (tagResult.size() - 2 >= 0
					&& tagResult.get(tagResult.size() - 2).charAt(0) == 'b') {
				return segResult.get(segResult.size() - 2)
						+ segResult.get(segResult.size() - 1);
			} else {
				return segResult.get(segResult.size() - 1);
			}
		} else {
			if (tagResult.size() - 2 >= 0
					&& tagResult.get(tagResult.size() - 2).charAt(0) == 'v') {
				return segResult.get(segResult.size() - 2);
			} else {
				return segResult.get(segResult.size() - 1);
			}
		}
	}

	public double getSimilarity(Double[] word1, Double[] word2) {
		double numerator = 0;
		for (int i = 0; i < word1.length; i++) {
			double mul = word1[i] * word2[i];
			numerator += mul;
		}
		double ab1 = getAbsoluteValue(word1);
		double ab2 = getAbsoluteValue(word2);
		return numerator / (ab1 * ab2);
	}

	private double getAbsoluteValue(Double[] vec) {
		double sum = 0;
		for (int i = 0; i < vec.length; i++) {
			sum += Math.pow(vec[i], 2);
		}
		return Math.sqrt(sum);
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.library.path"));
		try {
			CommonConfig.Initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String cws = CommonConfig.getString("cws");
		String pos = CommonConfig.getString("pos");

		if (Segmentor.create(cws) < 0 || Postagger.create(pos) < 0) {
			System.err.println("load failed");
		}

		VecSimilarity tool = new VecSimilarity(CommonConfig.getString("DIM"),
				CommonConfig.getString("W2V"));
		// Double[] wd1 = tool.getWordembedding("科学部", 1);
		// Double[] wd2 = tool.getWordembedding("总裁", 1);
		//
		// System.out.println(tool.getSimilarity(wd1, wd2));

		tool.getClose(args[0]);
	}

	public void getClose(String word) {
		ArrayList<String[]> score = new ArrayList<String[]>();
		Double[] wd1 = getWordembedding(word, 1);
		for (String item : wordembedding.keySet()) {
			double re = getSimilarity(wd1, wordembedding.get(item));
			String[] temp = new String[2];
			temp[0] = item;
			temp[1] = re + "";
			score.add(temp);
		}
		Collections.sort(score, new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2) {
				if (Double.parseDouble(o2[1]) > Double.parseDouble(o1[1])) {
					return 1;
				} else {
					if (Double.parseDouble(o2[1]) < Double.parseDouble(o1[1])) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});
		for (int i = 0; i < score.size(); i++) {
			double re = Double.parseDouble(score.get(i)[1]);
			System.out.println(score.get(i)[0] + ":" + re);
			if (i == 10) {
				break;
			}
		}
	}
}
