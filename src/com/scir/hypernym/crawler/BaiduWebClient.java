package com.scir.hypernym.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.RedirectListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.scir.hypernym.config.CommonConfig;
import com.scir.hypernym.webservice.VecSimilarity;

import edu.hit.ir.ltp4j.Segmentor;
import edu.hit.ir.ltp4j.Postagger;

/**
 * Web访问，用于获取搜索结果、百科页面等
 * 
 * @author Ricky
 * 
 */
public class BaiduWebClient {
	private static int numEachPage = 10; // 搜索结果每页结果条数
	private static int FRONT = 50;
	private static int FRONT2 = 50;
	private static HashMap<String, Integer> IDF = null;
	private static int MAXWORDNUM = 8;
	private static double THRESHOLD = 0.7;

	public static int getFRONT() {
		return FRONT;
	}

	public static int getFRONT2() {
		return FRONT2;
	}

	public static void setFRONT2(int fRONT2) {
		FRONT2 = fRONT2;
	}

	public static void setFRONT(int fRONT) {
		FRONT = fRONT;

	}

	private static String searchURL;
	private static String baiduBaikeURL;
	private static String hudongBaikeURL;
	private static VecSimilarity tool = null;

	// private static Pattern searchResultPattern;

	/**
	 * 初始化
	 */
	public static void Initialize() {
		try {
			CommonConfig.Initialize();

			// 加载ltp分词模型
			String cws = CommonConfig.getString("cws");
			String pos = CommonConfig.getString("pos");

			if (Segmentor.create(cws) < 0 || Postagger.create(pos) < 0) {
				System.err.println("load failed");
			}

			FRONT = Integer.parseInt(CommonConfig.getString("FRONT"));
			System.out.println("FRONT is " + FRONT);
			FRONT2 = Integer.parseInt(CommonConfig.getString("FRONT2"));
			System.out.println("FRONT2 is " + FRONT2);
			searchURL = "http://www.baidu.com/s";
			baiduBaikeURL = "http://baike.baidu.com/search/word";
			hudongBaikeURL = "http://www.baike.com/wiki/";

			IDF = new HashMap<String, Integer>();

			File file = new File("./Resource/words_idf.txt");

			System.out.println("读取词语IDF");
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String tempString = null;

				// 一次读入一行，直到读入null为文件结束
				while ((tempString = reader.readLine()) != null) {
					int idx = -1;
					int i = tempString.length() - 1;
					while (i >= 0) {
						if (tempString.charAt(i) == ':') {
							idx = i;
							break;
						}
						i--;
					}
					if (idx != -1) {
						String head = tempString.substring(0, idx);
						String tail = tempString.substring(idx + 1,
								tempString.length());

						try {
							IDF.put(head, Integer.parseInt(tail));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						continue;
					}
				}
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
			System.out.println("读取词语IDF完毕");

			List<Entry<String, Integer>> letmesee = new ArrayList<Entry<String, Integer>>(
					IDF.entrySet());

			Collections.sort(letmesee,
					new Comparator<Map.Entry<String, Integer>>() {
						public int compare(Map.Entry<String, Integer> o1,
								Map.Entry<String, Integer> o2) {
							if (o2.getValue() > o1.getValue()) {
								return 1;
							} else {
								if (o2.getValue() < o1.getValue()) {
									return -1;
								} else {
									return 0;
								}
							}
						}
					});
			for (int x = 0; x < 50; x++) {
				System.out.println(letmesee.get(x).getKey() + ":"
						+ letmesee.get(x).getValue());
			}

			tool = new VecSimilarity(CommonConfig.getString("DIM"),
					CommonConfig.getString("W2V"));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 调用搜索引擎搜索query
	 * 
	 * @param query
	 * @param ResultUnit
	 * @throws Exception
	 */
	public static void search(String query, String qbefore, String qcontent,
			String qafter, Integer type, OutputStreamWriter osw,
			OutputStreamWriter osw_score) throws Exception {
		HttpClient client = new HttpClient();
		client.start();

		HashMap<String, String> parMap = new HashMap<String, String>();
		parMap.put("wd", query);
		parMap.put("rn", String.valueOf(numEachPage));
		String url = genFullUrl(searchURL, parMap, "UTF-8");

		System.out.println("baidu search url is" + url);

		ContentExchange exchange = new ContentExchange(true);
		exchange.setMethod("GET");// POST
		exchange.setURL(url);

		client.send(exchange);

		// Waits until the exchange is terminated
		int exchangeState = exchange.waitForDone();

		if (exchangeState == HttpExchange.STATUS_COMPLETED) {
			// System.err.println(url);
			System.out.println(query + " : Success!");
			// 处理百度搜索结果
			parseSearchResult(exchange.getResponseContent(), query, qbefore,
					qcontent, qafter, type, osw, osw_score);
		} else if (exchangeState == HttpExchange.STATUS_EXCEPTED)
			// handleError();
			System.err.println(exchange.getResponseStatus());
		else if (exchangeState == HttpExchange.STATUS_EXPIRED)
			// handleSlowServer();
			System.err.println(exchange.getResponseStatus());

		client.stop();
	}

	/**
	 * 生成完整的URL
	 * 
	 * @param urlPrefix
	 *            the url prefix
	 * @param parMap
	 *            the par map
	 * @param charset
	 *            the charset
	 * 
	 * @return the string
	 */

	public static String genFullUrl(String urlPrefix,
			HashMap<String, String> parMap, String charset) {
		String newUrl = urlPrefix + "?";

		Iterator<Entry<String, String>> it = parMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> element = it.next();
			String parVal = element.getValue();
			String newParVal = "";
			try {
				newParVal = URLEncoder.encode(parVal, charset);
			} catch (UnsupportedEncodingException e) {
				// log
				e.printStackTrace();
			}
			newUrl += element.getKey() + "=" + newParVal;
			newUrl += "&";
		}
		return newUrl.substring(0, newUrl.length() - 1);
	}

	/**
	 * 解析搜索结果，抽取URL、Title和snippet
	 * 
	 * @param result
	 *            搜索结果文本
	 * @param ResultUnitVector
	 *            组织为结构化的搜索结果
	 */
	private static void parseSearchResult(String result, String q,
			String qbefore, String qcontent, String qafter, int type,
			OutputStreamWriter osw, OutputStreamWriter osw_score) {

		// \s+匹配任意空白字符
		result = result.replaceAll("\n", "").replaceAll("\\s+", " ");

		// Matcher m = searchResultPattern.matcher(result);

		Document resultDoc = Jsoup.parse(result);
		// 通过类名称来寻找
		// h3标签，class=t的标签
		Elements h3Links = resultDoc.select("h3.t");
		int i = 0;
		// Vector<String> titles = new Vector<String>();
		// Vector<String> snips = new Vector<String>();

		Vector<String> sentences = new Vector<String>();

		HashMap<String, Integer> synonyms = new HashMap<String, Integer>();

		try {
			for (Element element : h3Links) {
				// 有时候网页会多爬取一个title（下标为50），而这个title没有对应的snip，会导致NULL错误
				if (i >= numEachPage) {
					break;
				}
				Elements synonymsbb = element.select("em");

				for (int x = 0; x < synonymsbb.size(); x++) {
					if (synonyms.containsKey(synonymsbb.get(x).text())) {
						continue;
					}
					// -1、0、1、2
					int num = judgeSynonyms(synonymsbb.get(x).text(), qbefore,
							qafter);
					synonyms.put(synonymsbb.get(x).text(), num);
				}

				String title = element.text();
				// System.out.println("第" + i + "个br.title is " + title);
				collectSentence(title, sentences);
				// getSentenceScore(title, q, saigo);

				Elements synonymsbb2 = element.nextElementSibling()
						.select("em");

				for (int x = 0; x < synonymsbb2.size(); x++) {
					// System.out.println("第" + i + "个synonyms is "
					// + synonymsbb2.get(x).text());
					if (synonyms.containsKey(synonymsbb2.get(x).text())) {
						continue;
					}
					// -1、0、1、2
					int num = judgeSynonyms(synonymsbb2.get(x).text(), qbefore,
							qafter);
					synonyms.put(synonymsbb2.get(x).text(), num);
				}

				String snippet = element.nextElementSibling().text();
				// System.out.println("第" + i + "个br.snippet is " + snippet);
				// getSentenceScore(snippet, q, saigo);
				// snips.add(snippet);
				collectSentence(snippet, sentences);
				i++;
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		if (i < numEachPage) {
			System.out.print("搜索结果太少，去除该三元组");
			sentences.clear();
		}

		synonyms.put(qbefore, 0);
		synonyms.put(qafter, 1);

		HashMap<String, Integer> words = new HashMap<String, Integer>();

		for (String item : sentences) {
			if (judgeSentence(item, synonyms)) {
				getWords(item, words, type);
			}
		}

		// words需要去掉停止词，
		File file = new File("./Resource/stopwords.txt");
		BufferedReader reader = null;

		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;

			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {

				Iterator<Map.Entry<String, Integer>> wordit = words.entrySet()
						.iterator();
				while (wordit.hasNext()) {
					Map.Entry<String, Integer> word = wordit.next();

					if (isContain(tempString, word.getKey())) {
						wordit.remove();
					}
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// words还需要去掉同义词

		for (Map.Entry<String, Integer> item : synonyms.entrySet()) {

			Iterator<Map.Entry<String, Integer>> wordit = words.entrySet()
					.iterator();
			while (wordit.hasNext()) {
				Map.Entry<String, Integer> word = wordit.next();

				if (isContain(word.getKey(), item.getKey())
						|| isContain(item.getKey(), word.getKey())) {
					wordit.remove();
				}
			}
		}

		words.remove(qbefore);
		words.remove(qafter);

		int total = getTotal(words);

		words.put("total", total);

		// Comparator<Score> ct = new MyCompare1();
		// Collections.sort(words, ct);

		HashMap<String, Double[]> saigo = new HashMap<String, Double[]>();

		for (Entry<String, Integer> item : words.entrySet()) {
			Double[] score = new Double[2];

			if (total == 0) {
				continue;
			}
			double idfScore = getTFIDF(item.getKey(),
					(double) (item.getValue()) / total);

			// System.out.println(idfScore);
			score[0] = idfScore;

			// 算与关系指示词的关系度
			Double[] wd1 = tool.getWordembedding(qcontent, type);
			Double[] wd2 = tool.getWordembedding(item.getKey(), type);
			double sim = -1.0;

			if (!(wd1 == null || wd2 == null)) {
				sim = tool.getSimilarity(wd1, wd2);
			}

			score[1] = sim;

			saigo.put(item.getKey(), score);
		}

		List<Entry<String, Double[]>> list = new ArrayList<Entry<String, Double[]>>(
				saigo.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Double[]>>() {
			public int compare(Map.Entry<String, Double[]> o1,
					Map.Entry<String, Double[]> o2) {
				if (o2.getValue()[0] > o1.getValue()[0]) {
					return 1;
				} else {
					if (o2.getValue()[0] < o1.getValue()[0]) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});

		// 输出三元组每个词分数
		double doubleNum = 0.0;

		try {
			int j = 0;
			for (Entry<String, Double[]> item : list) {
				// System.out.print(item.getKey() + ":");

				// System.out.println(item.getValue());

				osw.write(item.getKey() + ":" + item.getValue()[0] + "\t"
						+ item.getValue()[1] + "\t");

				if (item.getValue()[1] >= 0) {
					doubleNum += Math.pow(item.getValue()[1], 2);
				}
				j++;

				if (j == MAXWORDNUM) {
					break;
				}
			}
			osw.flush();

			double finalScore = getFinalScore(list);

			osw_score.write(finalScore + "\t"
					+ (finalScore >= THRESHOLD ? true : false) + "\t" + type);
			osw_score.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static private double getFinalScore(double doubleNum,
			List<Entry<String, Double[]>> list) {
		double result = 0.0;
		int j = 0;
		for (Entry<String, Double[]> item : list) {
			double weight = 0.0;
			if (item.getValue()[1] >= 0) {
				weight = Math.pow(item.getValue()[1], 2) / doubleNum;
				result += weight * item.getValue()[1];
			}
			j++;
			if (j == MAXWORDNUM) {
				break;
			}
		}
		return result;
	}

	static private double getFinalScore(List<Entry<String, Double[]>> list) {
		double max = 0.0;
		int j = 0;
		for (Entry<String, Double[]> item : list) {
			if (item.getValue()[1] > max) {
				max = item.getValue()[1];
			}
			j++;
			if (j == MAXWORDNUM) {
				break;
			}
		}
		return max;
	}

	static private int getTotal(HashMap<String, Integer> words) {
		int total = 0;
		for (Integer i : words.values()) {
			total += i;
		}
		return total;
	}

	static private void collectSentence(String sentence, Vector<String> s) {
		String[] spre = sentence.split(",|，|。|\\.");

		for (int i = 0; i < spre.length; i++) {
			if (spre[i].equals("")) {
				continue;
			}
			s.add(spre[i]);
		}

	}

	static private double getTFIDF(String word, double TF) {
		int num = 1;
		Object ob = IDF.get(word);
		if (ob == null) {
			num = 1;
		} else {
			num = (int) ob;
		}

		int total = IDF.get("total");
		double divide = (double) total / num;
		double log = Math.log(divide);
		return TF * log;
	}

	static private void getWords(String item, HashMap<String, Integer> words,
			int sign) {
		ArrayList<String> segResult = new ArrayList<String>();
		Segmentor.segment(item, segResult);

		ArrayList<String> tagResult = new ArrayList<String>();

		Postagger.postag(segResult, tagResult);
		for (int i = 0; i < segResult.size(); i++) {
			String word = segResult.get(i);
			word = word.replaceAll("\\s+", "").replaceAll(" +", "");
			if (word.equals("\t") || word.equals("")) {
				continue;
			}
			// 以谓词为中心
			if (sign == 0) {
				if (tagResult.get(i).equals("nh")
						|| (tagResult.get(i).charAt(0) != 'v')) {
					continue;
				}
			} else {// 形容词直接跳过
				if (tagResult.get(i).equals("nh")
						|| (tagResult.get(i).charAt(0) != 'n' && tagResult.get(
								i).charAt(0) != 'b')) {
					continue;
				}
			}
			if (words.containsKey(word)) {
				words.put(word, words.get(word) + 1);
			} else {
				words.put(word, 1);
			}
		}
	}

	static private boolean judgeSentence(String sen,
			HashMap<String, Integer> synonyms) {
		int sign1 = 0;
		int sign2 = 0;
		for (String key : synonyms.keySet()) {
			if (sen.indexOf(key) != -1) {
				if (synonyms.get(key) == 0) {
					sign1 = 1;
				}
				if (synonyms.get(key) == 1) {
					sign2 = 1;
				}
				if (synonyms.get(key) == 2) {
					sign2 = 1;
					sign1 = 1;
				}
			}
		}

		if (sign1 == 1 || sign2 == 1) {
			return true;
		} else {
			return false;
		}
	}

	static private int judgeSynonyms(String synonym, String first, String second) {
		int sign1 = 0, sign2 = 0;
		if (isContain(synonym, second) || isContain(second, synonym)) {
			sign2 = 1;
		}
		if (isContain(synonym, first) || isContain(first, synonym)) {
			sign1 = 1;
		}
		if (sign1 == 1 && sign2 == 1) {
			return 2;
		} else {
			if (sign1 == 1) {
				return 0;
			}
			if (sign2 == 1) {
				return 1;
			}
			return -1;
		}
	}

	static private boolean isContain(String first, String second) {
		char[] firstA = first.toCharArray();
		char[] secondA = second.toCharArray();
		int num = 0;
		for (int i = 0; i < firstA.length; i++) {
			for (int j = 0; j < secondA.length; j++) {
				if (firstA[i] == secondA[j]) {
					num++;
					break;
				}
			}
		}
		if (num == firstA.length) {
			return true;
		}
		return false;
	}

	/**
	 * 在百度百科中搜索
	 * 
	 * @param query
	 *            查询
	 * @return baikePage 百度百科页面
	 * @throws Exception
	 */
	public static String searchInBaidubaike(String query) throws Exception {
		HashMap<String, String> parMap = new HashMap<String, String>();
		parMap.put("word", query);
		parMap.put("pic", "1");
		parMap.put("sug", "1");
		String baikePage = getWebPage(baiduBaikeURL, parMap);

		return baikePage;
	}

	/**
	 * 搜索互动百科
	 * 
	 * @param query
	 *            查询
	 * @return 互动百科页面
	 * @throws Exception
	 */
	public static String searchInHudongBaike(String query) throws Exception {
		String itemUrl = hudongBaikeURL + URLEncoder.encode(query, "UTF8");
		return getWebPage(itemUrl, new HashMap<String, String>());
	}

	/**
	 * 获取网页页面内容
	 * 
	 * @param urlPrefix
	 *            URL前缀
	 * @param parMap
	 *            参数
	 * @return page 网页内容
	 * @throws Exception
	 */
	private static String getWebPage(String urlPrefix,
			HashMap<String, String> parMap) throws Exception {
		if (urlPrefix.equals(""))
			return "";

		HttpClient client = new HttpClient();
		client.registerListener(RedirectListener.class.getName());
		client.start();
		String url = genFullUrl(urlPrefix, parMap, "UTF8").replaceAll("%2B",
				"+");

		System.err.println("百科标签 " + url);

		ContentExchange exchange = new ContentExchange(true);
		exchange.setMethod("GET");// POST
		exchange.setURL(url);

		client.send(exchange);

		// Waits until the exchange is terminated
		int exchangeState = exchange.waitForDone();

		String page = "";

		if (exchangeState == HttpExchange.STATUS_COMPLETED) {
			// doSomething();
			page = exchange.getResponseContent();
		} else if (exchangeState == HttpExchange.STATUS_EXCEPTED)
			// handleError();
			System.err.println(exchange.getResponseStatus());
		else if (exchangeState == HttpExchange.STATUS_EXPIRED)
			// handleSlowServer();
			System.err.println(exchange.getResponseStatus());

		client.stop();

		if (page == null) {
			page = "";
		}

		return page;
	}

	public static String searchInBaidubaikePoly(String url) throws Exception {
		HashMap<String, String> parMap = new HashMap<String, String>();
		parMap.put("force", "1");

		return getWebPage("http://baike.baidu.com" + url, parMap);
	}

	public static void main(String[] args) throws Exception {
		// CommonConfig.Initialize();
		BaiduWebClient.Initialize();

		int num = judgeSynonyms("中科院裴钢", "中科院", "裴钢");
		System.out.println(num);
	}
}
